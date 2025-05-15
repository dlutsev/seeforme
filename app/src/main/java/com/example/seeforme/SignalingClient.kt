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
    private val userRole: String, // 'volunteer' или 'blind'
    private val listener: SignalingListener
) {
    interface SignalingListener {
        fun onOfferReceived(offer: String)
        fun onAnswerReceived(answer: String)
        fun onIceCandidateReceived(candidate: String)
        fun onCallMatched(targetUser: String) // Новый метод для уведомления о соединении
        fun onCallRequest(fromUser: String) // Новый метод для волонтеров
        fun onCallEnded(reason: String) // Уведомление об окончании звонка
        fun onQueueUpdate(position: Int) // Обновление позиции в очереди
    }

    private val client = OkHttpClient()
    private lateinit var webSocket: WebSocket
    private var isLoggedIn = false
    private var onLoginCompleteListener: (() -> Unit)? = null

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SignalingClient", "WebSocket connected")
                sendLogin(userName, userRole)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SignalingClient", "Message received: $text")
                val data = JSONObject(text)
                when (data.getString("type")) {
                    "offer" -> listener.onOfferReceived(data.getString("offer"))
                    "answer" -> listener.onAnswerReceived(data.getString("answer"))
                    "candidate" -> listener.onIceCandidateReceived(data.getString("candidate"))
                    "login" -> handleLoginResponse(data)
                    "call_matched" -> listener.onCallMatched(data.getString("target"))
                    "call_request" -> listener.onCallRequest(data.getString("from"))
                    "call_ended" -> listener.onCallEnded(data.getString("reason"))
                    "queued" -> listener.onQueueUpdate(data.getInt("position"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SignalingClient", "WebSocket error: ${t.message}")
            }
        })
    }

    fun handleLoginResponse(data: JSONObject) {
        val success = data.optBoolean("success", false)
        if (success) {
            isLoggedIn = true
            Log.d("SignalingClient", "Login successful for user: $userName, role: $userRole")
            onLoginCompleteListener?.invoke()
        } else {
            Log.e("SignalingClient", "Login failed: ${data.optString("message")}")
        }
    }

    fun getUserRole(): String {
        return userRole
    }

    fun setOnLoginCompleteListener(listener: () -> Unit) {
        onLoginCompleteListener = listener
    }

    fun sendLogin(userName: String, role: String) {
        Log.d("SignalingClient", "Sending login request for user: $userName as $role")
        webSocket.send(
            JSONObject().apply {
                put("type", "login")
                put("name", userName)
                put("role", role)
            }.toString()
        )
    }

    // Запрос звонка (для слепых пользователей)
    fun requestCall() {
        if (isLoggedIn && userRole == "blind") {
            Log.d("SignalingClient", "Requesting call")
            webSocket.send(
                JSONObject().apply {
                    put("type", "request_call")
                }.toString()
            )
        } else {
            Log.e("SignalingClient", "Cannot request call: not logged in or not a blind user")
        }
    }

    // Волонтер сообщает о готовности принимать звонки
    fun reportVolunteerReady() {
        if (isLoggedIn && userRole == "volunteer") {
            Log.d("SignalingClient", "Volunteer reporting ready")
            webSocket.send(
                JSONObject().apply {
                    put("type", "volunteer_ready")
                }.toString()
            )
        } else {
            Log.e("SignalingClient", "Cannot report ready: not logged in or not a volunteer")
        }
    }

    fun sendOffer(targetUser: String, offer: String) {
        if (isLoggedIn) {
            Log.d("SignalingClient", "Sending offer to $targetUser: $offer")
            webSocket.send(
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
            webSocket.send(
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
                webSocket.send(
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

    fun sendLeave(targetUser: String) {
        if (isLoggedIn) {
            Log.d("SignalingClient", "Sending leave to $targetUser")
            webSocket.send(
                JSONObject().apply {
                    put("type", "leave")
                    put("target", targetUser)
                }.toString()
            )
        }
    }
}