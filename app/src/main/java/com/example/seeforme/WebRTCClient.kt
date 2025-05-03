import android.content.Context
import org.json.JSONObject
import org.webrtc.*
import android.os.Handler
import android.os.Looper
import android.util.Log

class WebRTCClient(
    private val context: Context,
    private val eglBase: EglBase,
    private val remoteRenderer: SurfaceViewRenderer,
    private val signalingClient: SignalingClient
) {

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    var peerConnection: PeerConnection? = null
    private lateinit var localVideoSource: VideoSource
    private lateinit var videoCapturer: VideoCapturer
    private var remoteDescriptionSet = false
    private var isAnswerReceived = false
    private var localAudioTrack: AudioTrack? = null

    fun initVideoRenderer(localRenderer: SurfaceViewRenderer, remoteRenderer: SurfaceViewRenderer) {
        Log.d("WebRTCClient", "Initializing video renderers")
        localRenderer.init(eglBase.eglBaseContext, null)
        localRenderer.setMirror(true)

        remoteRenderer.init(eglBase.eglBaseContext, null)
        remoteRenderer.setMirror(false)

        initPeerConnectionFactory()
        createLocalTrack(localRenderer)
    }

    fun setRemoteDescription(sdp: String, type: SessionDescription.Type) {
        Log.d("WebRTCClient", "Setting remote description: $sdp")
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), SessionDescription(type, sdp))
        remoteDescriptionSet = true
    }

    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    fun isRemoteDescriptionSet(): Boolean {
        return remoteDescriptionSet
    }
    fun addIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        if (remoteDescriptionSet) {
            Log.d("WebRTCClient","Candidate to peer connection")
            peerConnection?.addIceCandidate(candidate)
        } else {
            pendingIceCandidates.add(candidate)
            Log.d("WebRTCClient","Candidate to queue")
        }
    }

    private fun initPeerConnectionFactory() {
        Log.d("WebRTCClient", "Initializing PeerConnectionFactory")
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }


    private fun createLocalTrack(localRenderer: SurfaceViewRenderer) {
        Log.d("WebRTCClient", "Creating local video track")
        videoCapturer = createVideoCapturer()
        localVideoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
        videoCapturer.initialize(
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
            context,
            localVideoSource.capturerObserver
        )
        videoCapturer.startCapture(1920, 1080, 60)

        localVideoTrack = peerConnectionFactory.createVideoTrack("LOCAL_VIDEO_TRACK", localVideoSource)
        localAudioTrack = peerConnectionFactory.createAudioTrack("LOCAL_AUDIO_TRACK", peerConnectionFactory.createAudioSource(MediaConstraints()))
        localVideoTrack?.addSink(localRenderer)
    }

    private fun createVideoCapturer(): VideoCapturer {
        return Camera2Enumerator(context).run {
            deviceNames.firstOrNull { isFrontFacing(it) }?.let {
                Log.d("WebRTCClient", "Using front-facing camera: $it")
                createCapturer(it, null)
            } ?: throw IllegalStateException("No front-facing camera found.")
        }
    }

    fun createOffer(targetUser: String, signalingClient: SignalingClient, onOfferReady: (String) -> Unit) {
        Log.d("WebRTCClient", "Creating offer")
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        if (peerConnection == null) {
            peerConnection = createPeerConnection(targetUser, signalingClient)
        }
        peerConnection?.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO)?.setDirection(
            RtpTransceiver.RtpTransceiverDirection.SEND_RECV
        )
        peerConnection?.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO)?.setDirection(
            RtpTransceiver.RtpTransceiverDirection.SEND_RECV
        )
        peerConnection?.addTrack(localVideoTrack, listOf("streamId"))
        peerConnection?.addTrack(localAudioTrack, listOf("streamId"))
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(description: SessionDescription?) {
                description?.let {
                    Log.d("WebRTCClient", "Offer created: ${it.description}")
                    peerConnection?.setLocalDescription(SdpObserverAdapter(), it)
                    onOfferReady(it.description)
                }
            }

            override fun onCreateFailure(error: String?) {
                Log.e("WebRTCClient", "Failed to create offer: $error")
            }
        }, constraints)
    }

    fun createAnswer(targetUser: String, signalingClient: SignalingClient, onAnswerReady: (String) -> Unit) {
        Log.d("WebRTCClient", "Creating answer")
        if (peerConnection == null) {
            Log.d("WebRTCClient", "peerConnection is null. Creating a new one.")
            peerConnection = createPeerConnection(targetUser, signalingClient)
        }

        if (!isRemoteDescriptionSet()) {
            Log.e("WebRTCClient", "Remote description is not set, cannot create answer")
            return
        }
        peerConnection?.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO)?.setDirection(
            RtpTransceiver.RtpTransceiverDirection.SEND_RECV
        )
        peerConnection?.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO)?.setDirection(
            RtpTransceiver.RtpTransceiverDirection.SEND_RECV
        )
        peerConnection?.addTrack(localVideoTrack, listOf("streamId"))
        peerConnection?.addTrack(localAudioTrack, listOf("streamId"))
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(description: SessionDescription?) {
                description?.let {
                    Log.d("WebRTCClient", "Answer created: ${it.description}")
                    peerConnection?.setLocalDescription(SdpObserverAdapter(), it)
                    Log.d("WebRTCClient", "Local description set: ${it.description}")
                    onAnswerReady(it.description)
                }
            }

            override fun onCreateFailure(error: String?) {
                Log.e("WebRTCClient", "Failed to create answer: $error")
            }
        }, constraints)
    }




    fun createPeerConnection(targetUser: String, signalingClient: SignalingClient): PeerConnection? {
        Log.d("WebRTCClient", "Creating PeerConnection")
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),

        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        return peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d("WebRTCClient", "Generated ICE candidate: ${candidate.sdp}")
                if (targetUser == "user1"|| isAnswerReceived) {
                    signalingClient.sendCandidate(targetUser, JSONObject().apply {
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                        put("candidate", candidate.sdp)
                    }.toString())
                    Log.d("WebRTCClient", "Send ICE candidate to user1: ${candidate.sdp}")
                }
                else {
                    pendingIceCandidates.add(candidate)
                    Log.d("WebRTCClient", "ICE candidate added to queue")
                }
            }

            override fun onAddStream(stream: MediaStream) {
                if (stream.audioTracks.isNotEmpty()) stream.audioTracks[0].setEnabled(true)
                if (stream.videoTracks.isNotEmpty()) {
                    remoteVideoTrack = stream.videoTracks[0]
                    Log.d("WebRTCClient", "Stream added, attaching to renderer")
                    Handler(Looper.getMainLooper()).post {
                        remoteVideoTrack?.addSink(remoteRenderer)
                    }
                }
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Log.d("WebRTCClient", "ICE connection state changed: $newState")
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<MediaStream>) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: DataChannel) {}
            override fun onRenegotiationNeeded() {}
        })
    }

    fun onAnswerReceived(targetUser: String) {
        isAnswerReceived = true
        Log.d("WebRTCClient", "Answer received. Sending queued ICE candidates...")
        pendingIceCandidates.forEach { candidate ->
            signalingClient.sendCandidate(targetUser, JSONObject().apply {
                put("sdpMid", candidate.sdpMid)
                put("sdpMLineIndex", candidate.sdpMLineIndex)
                put("candidate", candidate.sdp)
            }.toString())
        }
        pendingIceCandidates.clear()
    }


    fun startLocalVideoCapture() {
        Log.d("WebRTCClient", "Starting local video capture")
        videoCapturer.startCapture(1920, 1080, 60)
    }

    fun endCall() {
        Log.d("WebRTCClient", "Ending call")
        if (::videoCapturer.isInitialized) {
            videoCapturer.stopCapture()
        }
        peerConnection?.close()
        peerConnection = null
    }

    fun release() {
        Log.d("WebRTCClient", "Releasing resources")
        if (::videoCapturer.isInitialized) {
            videoCapturer.stopCapture()
            videoCapturer.dispose()
        }
        localVideoSource.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        remoteVideoTrack?.dispose()
        peerConnection?.close()
        peerConnection = null
    }
}

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(description: SessionDescription?) {}
    override fun onSetSuccess() {
        Log.d("SdpObserverAdapter", "Set operation succeeded")
    }
    override fun onCreateFailure(error: String?) {
        Log.e("SdpObserverAdapter", "Create operation failed: $error")
    }
    override fun onSetFailure(error: String?) {
        Log.e("SdpObserverAdapter", "Set operation failed: $error")
    }
}

