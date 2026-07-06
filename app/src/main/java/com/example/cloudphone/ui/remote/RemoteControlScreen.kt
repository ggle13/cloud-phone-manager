package com.example.cloudphone.ui.remote

import android.Manifest
import android.content.pm.PackageManager
import android.view.TextureView
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cloudphone.data.model.CloudPhone
import com.example.cloudphone.data.webrtc.TouchAction
import com.example.cloudphone.data.webrtc.WebRTCSignalingClient
import com.example.cloudphone.ui.theme.CloudColors
import com.example.cloudphone.utils.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    navController: androidx.navigation.NavController,
    cloudPhone: CloudPhone,
    tokenManager: TokenManager
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var connectionState by remember { mutableStateOf("正在连接...") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    var showBackPressedDialog by remember { mutableStateOf(false) }

    // 获取当前 Token 和 userId
    val token = remember {
        runBlocking { tokenManager.getActiveTokenFlow().first() } ?: ""
    }
    val userId = remember {
        runBlocking { tokenManager.getActiveUserIdFlow().first() } ?: ""
    }
    val deviceId = remember { tokenManager.getActiveDeviceId() }

    // 视频渲染 SurfaceView
    val videoView = remember {
        org.webrtc.SurfaceViewRenderer(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setMirror(false)
            setEnableHardwareScaler(true)
        }
    }

    // WebRTC 客户端
    var signalingClient by remember { mutableStateOf<WebRTCSignalingClient?>(null) }

    // 权限申请
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // 权限都通过了，开始连接
        } else {
            errorMsg = "需要摄像头和音频权限才能远程控制"
        }
    }

    // 申请权限并连接
    LaunchedEffect(Unit) {
        val needed = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            // 开始连接
            startConnection()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    fun startConnection() {
        if (token.isEmpty()) {
            errorMsg = "Token 为空，请在 Token 管理页添加"
            return
        }
        if (userId.isEmpty()) {
            errorMsg = "UserId 为空，请在 Token 管理页展开卡片添加"
            return
        }

        val client = WebRTCSignalingClient(
            cloudPhone = cloudPhone,
            token = token,
            userId = userId,
            deviceId = deviceId,
            onVideoReady = { stream ->
                connectionState = "视频已就绪"
                isConnected = true
                showControls = true
                stream.videoTracks.firstOrNull()?.addSink(videoView)
            },
            onError = { err ->
                if (err.isNotEmpty()) errorMsg = err
            }
        )

        signalingClient = client
        client.connect()
        connectionState = "连接中..."
    }

    // 生命周期管理
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                signalingClient?.disconnect()
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!isConnected && signalingClient == null) {
                    startConnection()
                }
            }
            if (event == Lifecycle.Event.ON_DESTROY) {
                signalingClient?.disconnect()
                videoView.release()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            signalingClient?.disconnect()
            videoView.release()
        }
    }

    // 返回确认弹窗
    if (showBackPressedDialog) {
        AlertDialog(
            onDismissRequest = { showBackPressedDialog = false },
            shape = RoundedCornerShape(16.dp),
            title = { Text("退出控制？") },
            text = { Text("确定断开与「${cloudPhone.name}」的连接吗？") },
            confirmButton = {
                TextButton(onClick = {
                    signalingClient?.disconnect()
                    navController.popBackStack()
                }) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackPressedDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(cloudPhone.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            connectionState,
                            fontSize = 12.sp,
                            color = if (isConnected) CloudColors.CloudGreen
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showBackPressedDialog = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 重新连接
                    IconButton(onClick = {
                        signalingClient?.disconnect()
                        signalingClient = null
                        isConnected = false
                        errorMsg = null
                        startConnection()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重连")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.85f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // 视频渲染层
            AndroidView(
                factory = { videoView },
                modifier = Modifier.fillMaxSize()
            )

            // 加载中遮罩
            if (!isConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            connectionState,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                errorMsg!!,
                                color = Color.Red,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 触摸控制层（连接到云机后可操作）
            if (isConnected && showControls) {
                TouchOverlay(
                    onTouch = { action, x, y ->
                        signalingClient?.sendTouchEvent(action, x, y)
                    },
                    onKey = { keyCode, action ->
                        signalingClient?.sendKeyEvent(keyCode, action)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun TouchOverlay(
    onTouch: (TouchAction, Int, Int) -> Unit,
    onKey: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastTouchDown by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val x = offset.x.toInt()
                        val y = offset.y.toInt()
                        lastTouchDown = Pair(x, y)
                        onTouch(TouchAction.TOUCH_DOWN, x, y)
                        tryAwaitRelease()
                        onTouch(TouchAction.TOUCH_UP, x, y)
                        lastTouchDown = null
                    }
                )
            }
    ) {
        // 左下角快捷按钮
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Home 键
            ControlButton(
                icon = Icons.Default.Home,
                label = "Home",
                onClick = { onKey(3, "keyDown") }
            )
            // 返回键
            ControlButton(
                icon = Icons.Default.ArrowBack,
                label = "Back",
                onClick = { onKey(4, "keyDown") }
            )
            // 菜单键
            ControlButton(
                icon = Icons.Default.Menu,
                label = "Menu",
                onClick = { onKey(82, "keyDown") }
            )
        }

        // 右下角快捷按钮
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 锁屏
            ControlButton(
                icon = Icons.Default.PowerSettingsNew,
                label = "锁屏",
                onClick = { /* 发送锁屏命令 */ }
            )
        }
    }
}

@Composable
fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White.copy(alpha = 0.25f)
            )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
