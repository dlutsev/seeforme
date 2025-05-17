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
            userRole = data.optString("role")
            Log.d("SignalingClient", "Login successful for user: $userName, role: $userRole")
            onLoginCompleteListener?.invoke()
        } else {
            Log.e("SignalingClient", "Login failed: ${data.optString("message")}")
        }
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
            }.toString()
        )
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