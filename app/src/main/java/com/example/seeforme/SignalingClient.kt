package com.example.seeforme

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import android.util.Log

class SignalingClient(
    private val serverUrl: String,
    private val userName: String,
    private val listener: SignalingListener
) {
    interface SignalingListener {
        fun onOfferReceived(offer: String)
        fun onAnswerReceived(answer: String)
        fun onIceCandidateReceived(candidate: String)
    }

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var isLoggedIn = false
    private var onLoginCompleteListener: (() -> Unit)? = null
    private var onReadyListener: (() -> Unit)? = null
    private var onHelpAcceptedListener: ((String) -> Unit)? = null

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SignalingClient", "WebSocket connected")
                sendLogin(userName)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SignalingClient", "Message received: $text")
                try {
                val data = JSONObject(text)
                when (data.getString("type")) {
                    "offer" -> listener.onOfferReceived(data.getString("offer"))
                    "answer" -> listener.onAnswerReceived(data.getString("answer"))
                    "candidate" -> listener.onIceCandidateReceived(data.getString("candidate"))
                    "login" -> handleLoginResponse(data)
                    "ready" -> handleReadyMessage()
                    "help_request_sent" -> handleHelpRequestSent(data)
                    "help_accepted" -> handleHelpAccepted(data)
                    "connection_established" -> handleConnectionEstablished(data)
                    "user_disconnect" -> handleUserDisconnect(data)
                }
                } catch (e: Exception) {
                    Log.e("SignalingClient", "Error parsing message: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SignalingClient", "WebSocket closing: $code - $reason")
                isLoggedIn = false
                userRole = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SignalingClient", "WebSocket closed: $code - $reason")
                isLoggedIn = false
                userRole = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SignalingClient", "WebSocket error: ${t.message}")
                isLoggedIn = false
                userRole = null
            }
        })
    }

    private var userRole: String? = null

    fun handleLoginResponse(data: JSONObject) {
        val success = data.optBoolean("success", false)
        if (success) {
            isLoggedIn = true
            userRole = data.optString("role", data.optString("userType", "user"))
            Log.d("SignalingClient", "Login successful for user: $userName, role: $userRole")
            onLoginCompleteListener?.invoke()
        } else {
            Log.e("SignalingClient", "Login failed: ${data.optString("message")}")
        }
    }
    
    fun handleHelpRequestSent(data: JSONObject) {
        val requestId = data.optString("requestId")
        Log.d("SignalingClient", "Help request sent with ID: $requestId")
    }
    
    fun handleHelpAccepted(data: JSONObject) {
        val volunteerName = data.optString("volunteerName")
        Log.d("SignalingClient", "Help request accepted by volunteer: $volunteerName")
        onHelpAcceptedListener?.invoke(volunteerName)
    }
    
    fun handleConnectionEstablished(data: JSONObject) {
        val userId = data.optString("userId")
        Log.d("SignalingClient", "Connection established with user: $userId")
    }
    
    fun handleUserDisconnect(data: JSONObject) {
        val userName = data.optString("name")
        Log.d("SignalingClient", "User disconnected: $userName")
    }

    fun getUserRole(): String? {
        return userRole
    }

    fun handleReadyMessage() {
        onReadyListener?.invoke()
    }

    fun setOnReadyListener(listener: () -> Unit) {
        onReadyListener = listener
    }

    fun setOnLoginCompleteListener(listener: () -> Unit) {
        onLoginCompleteListener = listener
    }
    
    fun setOnHelpAcceptedListener(listener: (String) -> Unit) {
        onHelpAcceptedListener = listener
    }

    fun disconnect() {
        try {
            webSocket?.send(
                JSONObject().apply {
                    put("type", "leave")
                    put("name", userName)
                }.toString()
            )
            
            webSocket?.close(1000, "User disconnected")
            webSocket = null
            
            isLoggedIn = false
            userRole = null
            onLoginCompleteListener = null
            onReadyListener = null
            onHelpAcceptedListener = null
            
            Log.d("SignalingClient", "Disconnected from signaling server")
        } catch (e: Exception) {
            Log.e("SignalingClient", "Error during disconnect: ${e.message}")
        }
    }

    fun sendLogin(userName: String) {
        Log.d("SignalingClient", "Sending login request for user: $userName")
        webSocket?.send(
            JSONObject().apply {
                put("type", "login")
                put("name", userName)
                // Определяем тип пользователя (временно: если содержит "volunteer", то это волонтер)
                put("userType", if (userName.contains("volunteer")) "volunteer" else "user")
            }.toString()
        )
    }
    
    fun sendHelpRequest(question: String) {
        if (isLoggedIn) {
            Log.d("SignalingClient", "Sending help request: $question")
            webSocket?.send(
                JSONObject().apply {
                    put("type", "help_request")
                    put("question", question)
                }.toString()
            )
        } else {
            Log.e("SignalingClient", "Cannot send help request before login")
        }
    }
    
    fun acceptHelpRequest(requestId: String) {
        if (isLoggedIn && userRole == "volunteer") {
            Log.d("SignalingClient", "Accepting help request: $requestId")
            webSocket?.send(
                JSONObject().apply {
                    put("type", "accept_help")
                    put("requestId", requestId)
                }.toString()
            )
        } else {
            Log.e("SignalingClient", "Only logged in volunteers can accept help requests")
        }
    }

    fun sendOffer(targetUser: String, offer: String) {
        if (isLoggedIn) {
            Log.d("SignalingClient", "Sending offer to $targetUser: $offer")
            webSocket?.send(
                JSONObject().apply {
                    put("type", "offer")
                    put("target", targetUser)
                    put("offer", offer)
                }.toString()
            )
        } else {
            Log.e("SignalingClient", "Cannot send offer before login.")
        }
    }

    fun sendAnswer(targetUser: String, answer: String) {
        if (isLoggedIn) {
            Log.d("SignalingClient", "Sending answer to $targetUser: $answer")
            webSocket?.send(
                JSONObject().apply {
                    put("type", "answer")
                    put("target", targetUser)
                    put("answer", answer)
                }.toString()
            )
        } else {
            Log.e("SignalingClient", "Cannot send answer before login.")
        }
    }

    fun sendCandidate(targetUser: String, candidate: String) {
        if (isLoggedIn) {
            Log.d("SignalingClient", "Sending candidate to $targetUser: $candidate")
            try {
                webSocket?.send(
                    JSONObject().apply {
                        put("type", "candidate")
                        put("target", targetUser)
                        put("candidate", candidate)
                    }.toString()
                )
            } catch (e: Exception) {
                Log.e("SignalingClient", "Error sending candidate: ${e.message}")
            }
        } else {
            Log.e("SignalingClient", "Cannot send candidate before login.")
        }
    }
}