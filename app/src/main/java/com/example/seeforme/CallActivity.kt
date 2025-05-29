package com.example.seeforme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import org.webrtc.*
import com.example.seeforme.SignalingClient
import com.example.seeforme.WebRTCClient
import android.content.Intent

class CallActivity : AppCompatActivity() {

    private lateinit var webRTCClient: WebRTCClient
    private lateinit var localVideoView: SurfaceViewRenderer
    private lateinit var remoteVideoView: SurfaceViewRenderer
    private lateinit var eglBase: EglBase
    private lateinit var signalingClient: SignalingClient
    private lateinit var callHistoryService: CallHistoryService
    private lateinit var currentUser: String
    private lateinit var targetUser: String
    private var isCallSaved = false

    private val iceCandidatesQueue = mutableListOf<String>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        currentUser = intent.getStringExtra("currentUser") ?: return
        targetUser = intent.getStringExtra("targetUser") ?: return
        try {
            callHistoryService = CallHistoryService(applicationContext)
            Log.d("CallActivity", "CallHistoryService успешно инициализирован")
        } catch (e: Exception) {
            Log.e("CallActivity", "Ошибка при инициализации CallHistoryService: ${e.message}", e)
        }

        if (!WebRTCState.startCall(currentUser, targetUser)) {
            Toast.makeText(this, "Звонок уже активен", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        localVideoView = findViewById(R.id.local_video_view)
        remoteVideoView = findViewById(R.id.remote_video_view)
        eglBase = EglBase.create()

        signalingClient = SignalingClient("wss://seeforme.ru/signal", currentUser, object : SignalingClient.SignalingListener {
            override fun onOfferReceived(description: String) {
                Log.d("CallActivity", "Offer received: $description")
                runOnUiThread {
                    try {
                        if (webRTCClient.peerConnection == null) {
                            webRTCClient.peerConnection = webRTCClient.createPeerConnection(targetUser, signalingClient)
                        }
                        webRTCClient.setRemoteDescription(description, SessionDescription.Type.OFFER)
                        webRTCClient.createAnswer(targetUser, signalingClient) { answer ->
                            signalingClient.sendAnswer(targetUser, answer)
                        }
                    } catch (e: Exception) {
                        Log.e("CallActivity", "Error handling offer: ${e.message}")
                    }
                }
            }

            override fun onAnswerReceived(description: String) {
                Log.d("CallActivity", "Answer received: $description")
                runOnUiThread {
                    try {
                        webRTCClient.setRemoteDescription(description, SessionDescription.Type.ANSWER)
                        webRTCClient.onAnswerReceived(targetUser)
                    } catch (e: Exception) {
                        Log.e("CallActivity", "Error handling answer: ${e.message}")
                    }
                }
            }

            override fun onIceCandidateReceived(candidate: String) {
                Log.d("CallActivity", "ICE Candidate received: $candidate")
                runOnUiThread {
                    if (webRTCClient.isRemoteDescriptionSet()) {
                        val json = JSONObject(candidate)
                        webRTCClient.addIceCandidate(
                            json.getString("sdpMid"),
                            json.getInt("sdpMLineIndex"),
                            json.getString("candidate")
                        )
                    } else {
                        iceCandidatesQueue.add(candidate)
                    }
                }
            }
        })
        signalingClient.setOnCallEndedListener {
            Log.d("CallActivity", "Call ended by other participant")
            runOnUiThread {
                Toast.makeText(this, "Другой участник завершил звонок", Toast.LENGTH_SHORT).show()
                endCall()
                finish()
            }
        }

        webRTCClient = WebRTCClient(this, eglBase, remoteVideoView, signalingClient)
        webRTCClient.initVideoRenderer(localVideoView, remoteVideoView)
        checkPermissions()
        signalingClient.connect()
        
        signalingClient.setOnLoginCompleteListener {
            val role = signalingClient.getUserRole()
            Log.d("CallActivity", "Logged in with role: $role")
            signalingClient.setOnReadyListener {
                Log.d("CallActivity", "Ready event received, role: $role")
                if (role == "caller") {
                    webRTCClient.startLocalVideoCapture()
                    webRTCClient.createOffer(targetUser, signalingClient) { offer ->
                        signalingClient.sendOffer(targetUser, offer)
                    }
                } else if (role == "callee") {
                    webRTCClient.startLocalVideoCapture()
                }
            }
        }

        findViewById<ImageButton>(R.id.btn_end_call).setOnClickListener {
            Log.d("CallActivity", "Нажата кнопка завершения звонка")
            if (!isCallSaved) {
                try {
                    Log.d("CallActivity", "Сохраняем звонок в историю напрямую из обработчика кнопки")
                    saveCallToHistory()
                    isCallSaved = true
                    Log.d("CallActivity", "Звонок успешно сохранен в историю")
                } catch (e: Exception) {
                    Log.e("CallActivity", "Ошибка при сохранении звонка: ${e.message}", e)
                }
            }
            endCall()
            finish()
        }
        findViewById<ImageButton>(R.id.btn_switch_camera).setOnClickListener {
            webRTCClient.switchCamera()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    private fun endCall() {
        Log.d("CallActivity", "Завершение звонка. isCallSaved=$isCallSaved")
        webRTCClient.endCall()
        signalingClient.disconnect()
        WebRTCState.endCall()
        iceCandidatesQueue.clear()
        if (!isCallSaved) {
            saveCallToHistory()
            isCallSaved = true
            Log.d("CallActivity", "Звонок сохранен в историю. isCallSaved установлен в true")
        } else {
            Log.d("CallActivity", "Звонок уже был сохранен ранее, пропускаем сохранение")
        }
    }
    
    private fun saveCallToHistory() {
        if (!::callHistoryService.isInitialized) {
            Log.e("CallActivity", "Ошибка: callHistoryService не инициализирован. Пытаемся инициализировать.")
            try {
                callHistoryService = CallHistoryService(applicationContext)
                Log.d("CallActivity", "CallHistoryService успешно инициализирован в saveCallToHistory")
            } catch (e: Exception) {
                Log.e("CallActivity", "Не удалось инициализировать CallHistoryService: ${e.message}", e)
                return
            }
        }

        val isVolunteer = currentUser.startsWith("volunteer")
        val contactId: String
        val contactName: String
        Log.d("CallActivity", "Сохранение звонка в историю. Текущий пользователь: $currentUser, определенная роль isVolunteer: $isVolunteer")
        if (isVolunteer) {
            contactId = "user1"
            contactName = "Пользователь"
        } else {
            contactId = "volunteer1"
            contactName = "Волонтер"
        }
        
        try {
            Log.d("CallActivity", "Вызов метода saveCall: contactId=$contactId, contactName=$contactName, isVolunteer=$isVolunteer")
            callHistoryService.saveCall(contactId, contactName, isVolunteer)
            Log.d("CallActivity", "Метод saveCall успешно выполнен")
        } catch (e: Exception) {
            Log.e("CallActivity", "Ошибка при сохранении звонка: ${e.message}", e)
        }
        
        Log.d("CallActivity", "Звонок сохранен в историю: $contactName ($contactId) для ${if (isVolunteer) "волонтера" else "пользователя"}")
    }

    override fun onBackPressed() {
        Log.d("CallActivity", "onBackPressed вызван")
        endCall()
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CallActivity", "onDestroy вызван. isCallSaved=$isCallSaved")
        if (!isCallSaved) {
            Log.d("CallActivity", "onDestroy: звонок еще не был сохранен, вызываем endCall()")
            endCall()
        } else {
            Log.d("CallActivity", "onDestroy: звонок уже был сохранен, просто освобождаем ресурсы")
            webRTCClient.release()
            localVideoView.release()
            remoteVideoView.release()
            eglBase.release()
        }
    }
}
