package com.example.seeforme

import SignalingClient
import WebRTCClient
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.webrtc.*

class CallActivity : AppCompatActivity() {

    private lateinit var webRTCClient: WebRTCClient
    private lateinit var localVideoView: SurfaceViewRenderer
    private lateinit var remoteVideoView: SurfaceViewRenderer
    private lateinit var eglBase: EglBase
    private lateinit var signalingClient: SignalingClient

    private val iceCandidatesQueue = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        val currentUser = intent.getStringExtra("currentUser") ?: return
        val targetUser = intent.getStringExtra("targetUser") ?: return

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

        webRTCClient = WebRTCClient(this, eglBase, remoteVideoView, signalingClient)
        webRTCClient.initVideoRenderer(localVideoView, remoteVideoView)

        signalingClient.connect()
        signalingClient.setOnLoginCompleteListener {
            val role = signalingClient.getUserRole()
            //dumb test for 1v1 call
            signalingClient.setOnReadyListener {
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

        findViewById<Button>(R.id.btn_end_call).setOnClickListener {
            webRTCClient.endCall()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        eglBase.release()
        webRTCClient.release()
    }
}
