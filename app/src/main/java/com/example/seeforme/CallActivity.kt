package com.example.seeforme

import SignalingClient
import WebRTCClient
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.webrtc.*

class CallActivity : AppCompatActivity(), SignalingClient.SignalingListener {

    private lateinit var webRTCClient: WebRTCClient
    private lateinit var localVideoView: SurfaceViewRenderer
    private lateinit var remoteVideoView: SurfaceViewRenderer
    private lateinit var eglBase: EglBase
    private lateinit var signalingClient: SignalingClient

    private var currentUser: String = ""
    private var targetUser: String = ""
    private var userRole: String = ""
    private val iceCandidatesQueue = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        currentUser = intent.getStringExtra("currentUser") ?: return
        targetUser = intent.getStringExtra("targetUser") ?: return
        userRole = if (intent.getBooleanExtra("isFromNotification", false)) "volunteer" else "blind"
        
        Log.d("CallActivity", "Started call: currentUser=$currentUser, targetUser=$targetUser, role=$userRole")

        localVideoView = findViewById(R.id.local_video_view)
        remoteVideoView = findViewById(R.id.remote_video_view)
        eglBase = EglBase.create()

        // Инициализируем SignalingClient с нашими данными
        signalingClient = SignalingClient(
            "wss://seeforme.ru/v1/signal",
            currentUser,
            userRole,
            this
        )

        webRTCClient = WebRTCClient(this, eglBase, remoteVideoView, signalingClient)
        webRTCClient.initVideoRenderer(localVideoView, remoteVideoView)

        signalingClient.connect()
        signalingClient.setOnLoginCompleteListener {
            // Когда подключились к серверу сигналов
            webRTCClient.startLocalVideoCapture()
            
            if (userRole == "blind") {
                // Если мы слепой пользователь, инициируем звонок (отправляем оффер)
                Log.d("CallActivity", "Blind user initiating call to $targetUser")
                webRTCClient.createOffer(targetUser, signalingClient) { offer ->
                    signalingClient.sendOffer(targetUser, offer)
                }
            } else {
                Log.d("CallActivity", "Volunteer waiting for offer from $targetUser")
                // Волонтер ждет оффер
            }
        }

        findViewById<Button>(R.id.btn_end_call).setOnClickListener {
            // Сообщаем серверу об окончании звонка
            signalingClient.sendLeave(targetUser)
            webRTCClient.endCall()
            finish()
        }
    }
    
    // Реализация методов интерфейса SignalingListener
    override fun onOfferReceived(offer: String) {
        Log.d("CallActivity", "Offer received")
        runOnUiThread {
            try {
                if (webRTCClient.peerConnection == null) {
                    webRTCClient.peerConnection = webRTCClient.createPeerConnection(targetUser, signalingClient)
                }
                webRTCClient.setRemoteDescription(offer, SessionDescription.Type.OFFER)
                webRTCClient.createAnswer(targetUser, signalingClient) { answer ->
                    signalingClient.sendAnswer(targetUser, answer)
                }
            } catch (e: Exception) {
                Log.e("CallActivity", "Error handling offer: ${e.message}")
                Toast.makeText(this, "Ошибка при обработке предложения звонка", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onAnswerReceived(answer: String) {
        Log.d("CallActivity", "Answer received")
        runOnUiThread {
            try {
                webRTCClient.setRemoteDescription(answer, SessionDescription.Type.ANSWER)
                webRTCClient.onAnswerReceived(targetUser)
            } catch (e: Exception) {
                Log.e("CallActivity", "Error handling answer: ${e.message}")
                Toast.makeText(this, "Ошибка при обработке ответа на звонок", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onIceCandidateReceived(candidate: String) {
        Log.d("CallActivity", "ICE Candidate received")
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
    
    override fun onCallMatched(targetUser: String) {
        // Не используется в этой активности
    }
    
    override fun onCallRequest(fromUser: String) {
        // Не используется в этой активности
    }
    
    override fun onCallEnded(reason: String) {
        // Вызов был завершен удаленно
        runOnUiThread {
            Toast.makeText(this, "Звонок завершен: $reason", Toast.LENGTH_SHORT).show()
            webRTCClient.endCall()
            finish()
        }
    }
    
    override fun onQueueUpdate(position: Int) {
        // Не используется в этой активности
    }

    override fun onDestroy() {
        super.onDestroy()
        eglBase.release()
        webRTCClient.release()
    }
}
