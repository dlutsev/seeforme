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

    private val iceCandidatesQueue = mutableListOf<String>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        val currentUser = intent.getStringExtra("currentUser") ?: return
        val targetUser = intent.getStringExtra("targetUser") ?: return

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

        // Добавляем обработчик завершения звонка
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
            
            // Ждем события ready от сервера
            signalingClient.setOnReadyListener {
                Log.d("CallActivity", "Ready event received, role: $role")
                
                if (role == "caller") {
                    // Пользователь - инициатор звонка
                    webRTCClient.startLocalVideoCapture()
                    webRTCClient.createOffer(targetUser, signalingClient) { offer ->
                        signalingClient.sendOffer(targetUser, offer)
                    }
                } else if (role == "callee") {
                    // Волонтер - принимает звонок
                    webRTCClient.startLocalVideoCapture()
                }
            }
        }

        findViewById<ImageButton>(R.id.btn_end_call).setOnClickListener {
            endCall()
            finish()
        }
        
        // Обработчик кнопки переключения камеры
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
        webRTCClient.endCall()
        signalingClient.disconnect()
        WebRTCState.endCall()
        iceCandidatesQueue.clear()

    }

    override fun onBackPressed() {
        // Перехватываем нажатие кнопки "Назад" для корректного завершения звонка
        endCall()
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        endCall()
        localVideoView.release()
        remoteVideoView.release()
        eglBase.release()
        webRTCClient.release()
    }
}
