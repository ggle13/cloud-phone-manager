package com.example.cloudphone.data.webrtc

import android.util.Log
import com.example.cloudphone.data.model.CloudPhone
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import org.webrtc.*
import java.util.concurrent.TimeUnit

/**
 * 360 云手机 WebRTC 信令客户端（App API 版）
 *
 * 信令流程：
 *  1. 建立 WebSocket 连接（带 App header）
 *  2. 发送 request 消息请求连接云机
 *  3. 收到 response（含云机 SDP 参数 + 媒体地址）
 *  4. 创建本地 WebRTC offer，交换 SDP
 *  5. 交换 ICE Candidates
 *  6. 媒体流建立
 *
 * 触摸/按键通过 WebSocket 发送（peerid 必填）
 */
class WebRTCSignalingClient(
    private val cloudPhone: CloudPhone,
    private val token: String,
    private val userId: String,
    private val deviceId: String,
    private val onVideoReady: (MediaStream) -> Unit,
    private val onError: (String) -> Unit,
    private val onConnected: () -> Unit = {}
) {
    companion object {
        private const val TAG = "WebRTCSignaling"
        private const val WS_HOST = "signal.wo-adv.cn"
        private const val WS_PORT = 18443
        private const val WS_PATH = "/signalman/signal"
        private const val WS_ORIGIN = "https://uphone.wo-adv.cn"
    }

    private var webSocket: WebSocket? = null
    private var peerConnection: PeerConnection? = null
    private var currentMsgId: String = ""
    private var remotePeerId: String = ""        // csdk:remoteName
    private var remoteInstanceId: String = ""    // remoteName

    // 云机分辨率（来自 response）
    private var remoteWidth: Int = 1080
    private var remoteHeight: Int = 1920

    // EGL 环境用于视频渲染
    private val eglBase = EglBase.create()

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    /** 开始连接云机 */
    fun connect() {
        remoteInstanceId = cloudPhone.remoteName ?: cloudPhone.instanceNum
        remotePeerId = "csdk:$remoteInstanceId"

        val url = "wss://$WS_HOST:$WS_PORT$WS_PATH?client_id=$userId&component=app"
        Log.d(TAG, "连接信令服务器: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Origin", WS_ORIGIN)
            .addHeader("Upgrade", "websocket")
            .addHeader("Connection", "Upgrade")
            .addHeader("Sec-WebSocket-Version", "13")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket 已连接")
                sendConnectRequest()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "收到消息: ${text.take(300)}")
                handleSignalingMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "收到二进制消息: ${bytes.hex().take(100)}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket 连接失败: ${t.message}")
                onError("连接失败: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 关闭: $code $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 已关闭: $code $reason")
                disconnect()
            }
        })
    }

    /**
     * 发送连接请求（第一步）
     * 来自真实 App 抓包格式
     */
    private fun sendConnectRequest() {
        currentMsgId = "android_${System.currentTimeMillis()}"

        val json = JSONObject().apply {
            put("id", remoteInstanceId)
            put("type", "request")
            put("clienttype", "app")
            put("supporth265", false)
            put("cameraDataType", 1)
            put("peerid", remotePeerId)
            put("deviceid", deviceId)
            put("physical_resolution", JSONObject().apply {
                put("width", 1080)
                put("height", 1920)
            })
            put("virtual_resolution", JSONObject().apply {
                put("width", 1080)
                put("height", 1920)
            })
            put("packetization_mode", 1)
            put("token", token)
            put("userid", userId)
            put("cloudid", remoteInstanceId)
            put("bitrate", 5000000)
            put("gopSize", 300)
            put("profile", 2)        // Baseline
            put("rcMode", 3)
            put("frameRate", 60)
            put("msgid", currentMsgId)
            put("version", "1.1.10-lt")
            put("clientinfo", "Android App")
        }

        val sent = webSocket?.send(json.toString()) ?: false
        Log.d(TAG, "发送 request: $sent")
    }

    /**
     * 处理收到的信令消息
     */
    private fun handleSignalingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                // ===== App API：云机回复 SDP 参数 =====
                "response" -> {
                    val peerId = json.optString("peerid", "")
                    val instanceId = json.optString("instanceid", "")
                    val cphWidth = json.optInt("cphwidth", 1080)
                    val cphHeight = json.optInt("cphheight", 1920)
                    val useH265 = json.optBoolean("useh265", false)
                    val supportNewTouchCmd = json.optBoolean("supportNewTouchCmd", true)
                    val touchVersion = json.optInt("touchVersion", 1)
                    val outterIp = json.optString("outterip", "")
                    val outterPort4 = json.optInt("outterport4", 0)

                    Log.d(TAG, "=== 云机 response ===")
                    Log.d(TAG, "  peerid: $peerId")
                    Log.d(TAG, "  instanceid: $instanceId")
                    Log.d(TAG, "  分辨率: ${cphWidth}x${cphHeight}")
                    Log.d(TAG, "  H.265: $useH265")
                    Log.d(TAG, "  新触摸协议: $supportNewTouchCmd (v$touchVersion)")
                    Log.d(TAG, "  媒体地址: $outterIp:$outterPort4")

                    remoteWidth = cphWidth
                    remoteHeight = cphHeight

                    // 云机回复后，创建本地 WebRTC offer 建立连接
                    createPeerConnectionAndOffer(outterIp, outterPort4)
                }

                // ===== H5 API：云机发 offer =====
                "offer" -> {
                    val sdp = json.optString("sdp", "")
                    val msgId = json.optString("msgid", "")
                    if (sdp.isNotEmpty()) {
                        Log.d(TAG, "收到 Offer SDP，创建 Answer")
                        createAnswerFromOffer(sdp, msgId)
                    }
                }

                // ===== WebRTC answer（客户端发给云机）====
                "answer" -> {
                    // 通常 answer 是我们发给云机，云机不会主动发 answer
                    Log.d(TAG, "收到 answer 消息")
                }

                // ===== ICE Candidate 交换 =====
                "candidate", "ice" -> {
                    val sdpMid = json.optString("sdpMid", "")
                    val sdpMLineIndex = json.optInt("sdpMLineIndex", 0)
                    val candidate = json.optString("candidate", "")
                    if (candidate.isNotEmpty()) {
                        Log.d(TAG, "收到 ICE Candidate")
                        addRemoteIceCandidate(sdpMid, sdpMLineIndex, candidate)
                    }
                }

                // ===== 连接确认 =====
                "linkResponse" -> {
                    val isSuccess = json.optBoolean("isSuccess", false)
                    Log.d(TAG, "linkResponse: isSuccess=$isSuccess")
                    if (isSuccess) {
                        onConnected()
                    }
                }

                // ===== 错误处理 =====
                "error" -> {
                    val retcode = json.optInt("retcode", -1)
                    val retmsg = json.optString("retmsg", "未知错误")
                    Log.e(TAG, "服务端错误: [$retcode] $retmsg")
                    onError("[$retcode] $retmsg")
                }

                else -> {
                    Log.d(TAG, "收到未知类型: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析信令消息失败: ${e.message}")
        }
    }

    /**
     * 创建 PeerConnection 并生成 offer
     * 使用云机媒体地址作为 ICE candidate
     */
    private fun createPeerConnectionAndOffer(mediaIp: String, mediaPort: Int) {
        val factory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()

        // ICE 服务器：STUN + 云机媒体地址（来自 response）
        val iceServers = mutableListOf<PeerConnection.IceServer>()
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        if (mediaIp.isNotEmpty() && mediaPort > 0) {
            // 优先尝试直接连接云机媒体地址
            iceServers.add(
                PeerConnection.IceServer.builder("turn:$mediaIp:$mediaPort")
                    .setUsername("cloudphone")
                    .setPassword("cloudphone123")
                    .createIceServer()
            )
        }

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                sendIceCandidate(candidate)
            }

            override fun onAddStream(stream: MediaStream) {
                Log.d(TAG, "收到视频流: ${stream.id}")
                onVideoReady(stream)
                onConnected()
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>) {
                Log.d(TAG, "收到 track: ${receiver?.id()}")
                streams.firstOrNull()?.let { onVideoReady(it) }
                onConnected()
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "SignalingState: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE 连接状态: ${state?.name}")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> onConnected()
                    PeerConnection.IceConnectionState.FAILED -> onError("网络连接失败")
                    PeerConnection.IceConnectionState.DISCONNECTED -> onError("连接断开")
                    else -> {}
                }
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE 收集状态: ${state?.name}")
            }

            // 以下为空实现（必须 override）
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceCandidateError(error: CandidatePairChangedEvent?) {}
            override fun onStandardizedIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
        })

        // 创建本地 video track
        val videoSource = factory.createVideoSource(false)
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        val videoCapturer = Camera2Enumerator(android.app.Activity()).createCapturer(
            Camera2Enumerator.getDeviceNames().firstOrNull(), surfaceTextureHelper
        )
        videoCapturer?.initialize(surfaceTextureHelper, android.content.Context(), videoSource.capturerObserver)
        videoCapturer?.startCapture(remoteWidth, remoteHeight, 30)

        val localVideoTrack = factory.createVideoTrack("local_video", videoSource)
        val localStream = factory.createLocalMediaStream("local_stream")
        localStream.addTrack(localVideoTrack)
        peerConnection?.addTrack(localVideoTrack, listOf("local_stream"))

        // 创建 offer
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(TAG, "本地 offer SDP 创建成功，长度: ${sessionDescription.description.length}")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d(TAG, "本地 offer 设置成功")
                        // 发送 answer 给云机（App API 流程）
                        sendAnswerToCloudPhone(sessionDescription.description)
                    }
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "设置本地 SDP 失败: $error")
                        onError("SDP 设置失败: $error")
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sessionDescription)
            }
            override fun onSetFailure(error: String?) {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "创建 offer 失败: $error")
                onError("创建连接失败: $error")
            }
        }, MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        })
    }

    /**
     * 发送 answer SDP 给云机（App API 流程）
     * 云机发了 response 后，我们需要发 answer 确认
     */
    private fun sendAnswerToCloudPhone(answerSdp: String) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("peerid", remotePeerId)
            put("id", remoteInstanceId)
            put("sdp", answerSdp)
            put("msgid", currentMsgId)
        }
        val sent = webSocket?.send(json.toString()) ?: false
        Log.d(TAG, "发送 answer SDP: $sent")
    }

    /**
     * 从 offer 创建 answer（H5 API 流程）
     */
    private fun createAnswerFromOffer(offerSdp: String, msgId: String) {
        val factory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                sendIceCandidate(candidate)
            }
            override fun onAddStream(stream: MediaStream) { onVideoReady(stream); onConnected() }
            override fun onAddTrack(r: RtpReceiver?, s: Array<out MediaStream>) {
                s.firstOrNull()?.let { onVideoReady(it) }; onConnected()
            }
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE: ${state?.name}")
                if (state == PeerConnection.IceConnectionState.CONNECTED) onConnected()
                if (state == PeerConnection.IceConnectionState.FAILED) onError("ICE 连接失败")
            }
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>) {}
            override fun onRemoveStream(s: MediaStream?) {}
            override fun onDataChannel(d: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceConnectionReceivingChange(r: Boolean) {}
            override fun onIceCandidateError(e: CandidatePairChangedEvent?) {}
            override fun onStandardizedIceConnectionChange(s: PeerConnection.IceConnectionState?) {}
        })

        val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(sessionDescription: SessionDescription) {
                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                val json = JSONObject().apply {
                                    put("type", "answer")
                                    put("peerid", remotePeerId)
                                    put("id", remoteInstanceId)
                                    put("sdp", sessionDescription.description)
                                    put("msgid", msgId)
                                }
                                webSocket?.send(json.toString())
                                Log.d(TAG, "发送 answer")
                            }
                            override fun onSetFailure(e: String?) {}
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(e: String?) {}
                        }, sessionDescription)
                    }
                    override fun onSetFailure(e: String?) {}
                    override fun onCreateFailure(e: String?) {}
                }, MediaConstraints())
            }
            override fun onSetFailure(e: String?) {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(e: String?) {}
        }, offer)
    }

    /**
     * 添加远端 ICE Candidate
     */
    private fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        peerConnection?.addIceCandidate(iceCandidate)
    }

    /**
     * 发送本地 ICE Candidate 给云机
     */
    private fun sendIceCandidate(candidate: IceCandidate) {
        val json = JSONObject().apply {
            put("type", "candidate")
            put("peerid", remotePeerId)
            put("id", remoteInstanceId)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
            put("msgid", currentMsgId)
        }
        webSocket?.send(json.toString())
    }

    /**
     * 发送触摸事件
     * supportNewTouchCmd=true 时用新版格式
     */
    fun sendTouchEvent(action: TouchAction, x: Int, y: Int, touchId: Long = System.currentTimeMillis()) {
        val json = JSONObject().apply {
            put("type", "input")
            put("peerid", remotePeerId)
            put("id", remoteInstanceId)
            put("action", action.jsonValue)
            put("x", x)
            put("y", y)
            put("touchId", touchId)
            put("msgid", currentMsgId)
        }
        val sent = webSocket?.send(json.toString()) ?: false
        Log.d(TAG, "发送触摸 [$action] ($x, $y): $sent")
    }

    /**
     * 发送按键事件
     */
    fun sendKeyEvent(keyCode: Int, action: String = "keyDown") {
        val json = JSONObject().apply {
            put("type", "key")
            put("peerid", remotePeerId)
            put("id", remoteInstanceId)
            put("action", action)
            put("keyCode", keyCode)
            put("msgid", currentMsgId)
        }
        val sent = webSocket?.send(json.toString()) ?: false
        Log.d(TAG, "发送按键 [$action] keyCode=$keyCode: $sent")
    }

    /** 获取 EGL 环境（用于视频渲染）*/
    fun getEglBase() = eglBase

    /** 断开连接，释放资源 */
    fun disconnect() {
        try { peerConnection?.dispose(); peerConnection = null } catch (e: Exception) {}
        try { webSocket?.close(1000, "正常关闭"); webSocket = null } catch (e: Exception) {}
        try { eglBase.release() } catch (e: Exception) {}
    }
}

/** 触摸动作类型（touchVersion=1）*/
enum class TouchAction(val jsonValue: String) {
    TOUCH_DOWN("touchDown"),
    TOUCH_MOVE("touchMove"),
    TOUCH_UP("touchUp"),
    TAP("tap"),
    SWIPE("swipe")
}
