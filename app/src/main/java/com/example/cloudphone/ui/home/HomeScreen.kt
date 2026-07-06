package com.example.cloudphone.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.cloudphone.data.model.CloudPhone
import com.example.cloudphone.data.repository.CloudPhoneRepository
import com.example.cloudphone.ui.theme.CloudColors
import com.example.cloudphone.utils.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    tokenManager: TokenManager,
    repository: CloudPhoneRepository,
    onNavigateToTokens: () -> Unit,
    onNavigateToRemote: (String) -> Unit
) {
    val viewModel = remember { HomeViewModel(repository) }
    val phones by viewModel.phones.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val activeToken by viewModel.activeTokenRemark.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "云手机管理",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (activeToken.isNotEmpty()) {
                            Text(
                                "账号: $activeToken",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // 刷新按钮
                    IconButton(onClick = { viewModel.loadPhones() }) {
                        Icon(Icons.Default.Refresh, "刷新", tint = MaterialTheme.colorScheme.primary)
                    }
                    // 更多菜单
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("切换账号") },
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                                onClick = { showMenu = false; onNavigateToTokens() }
                            )
                            DropdownMenuItem(
                                text = { Text("添加账号") },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                                onClick = { showMenu = false; onNavigateToTokens() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("关于") },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadPhones() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading && phones.isEmpty() -> {
                    LoadingContent()
                }

                error != null && phones.isEmpty() -> {
                    ErrorContent(error!!) { viewModel.loadPhones() }
                }

                phones.isEmpty() -> {
                    EmptyContent(onNavigateToTokens)
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 统计卡片
                        item {
                            StatusSummary(phones)
                        }

                        items(phones, key = { "${it.id}_${it.status}" }) { phone ->
                            CloudPhoneCard(
                                phone = phone,
                                onRename = { viewModel.renamePhone(phone.id.toString(), it) },
                                onStart = { viewModel.startPhone(phone.id) },
                                onStop = { viewModel.stopPhone(phone.id) },
                                onReboot = { viewModel.rebootPhone(phone.id) },
                                onConnect = { cloudPhone ->
                                    val phoneJson = com.google.gson.Gson().toJson(cloudPhone)
                                    onNavigateToRemote(java.net.URLEncoder.encode(phoneJson, "UTF-8"))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel.message) {
        viewModel.message.collect { msg ->
            if (msg.isNotEmpty()) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearMessage()
            }
        }
    }
}

// ─── 加载中 ───
@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = CloudColors.Primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("加载中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── 错误页 ───
@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.ErrorOutline,
                null,
                modifier = Modifier.size(56.dp),
                tint = CloudColors.StatusError
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                error,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = CloudColors.Primary)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("重试")
            }
        }
    }
}

// ─── 空状态 ───
@Composable
private fun EmptyContent(onNavigateToTokens: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.PhoneAndroid,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("暂无云机", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "当前账号下没有云机",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onNavigateToTokens) {
                Text("切换账号")
            }
        }
    }
}

// ─── 状态统计 ───
@Composable
private fun StatusSummary(phones: List<CloudPhone>) {
    val online = phones.count { it.status in listOf("running", "online") }
    val offline = phones.count { it.status in listOf("stopped", "offline") }
    val other = phones.size - online - offline

    // 顶部欢迎卡片
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "概览",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("运行中", online.toString(), CloudColors.StatusOnline, Icons.Filled.CheckCircle)
                StatItem("已关机", offline.toString(), CloudColors.StatusOffline, Icons.Filled.PowerOff)
                if (other > 0) {
                    StatItem("其他", other.toString(), CloudColors.StatusPending, Icons.Filled.Schedule)
                }
                StatItem("总计", phones.size.toString(), CloudColors.Primary, Icons.Filled.Smartphone)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── 云机卡片 ───
@Composable
fun CloudPhoneCard(
    phone: CloudPhone,
    onRename: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReboot: () -> Unit,
    onConnect: (CloudPhone) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf<String?>(null) }

    val statusColor = when (phone.status.lowercase()) {
        "running", "online" -> CloudColors.StatusOnline
        "stopped", "offline" -> CloudColors.StatusOffline
        "starting", "stopping", "rebooting" -> CloudColors.StatusPending
        else -> CloudColors.StatusPending
    }

    val statusText = when (phone.status.lowercase()) {
        "running" -> "运行中"
        "online" -> "运行中"
        "stopped" -> "已关机"
        "offline" -> "已关机"
        "starting" -> "开机中"
        "stopping" -> "关机中"
        "rebooting" -> "重启中"
        else -> phone.status
    }

    val isOnline = phone.status.lowercase() in listOf("running", "online")

    // 动态呼吸灯动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isOnline) 4.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶部行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 呼吸灯状态点
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOnline) statusColor.copy(alpha = pulseAlpha)
                                else statusColor
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = phone.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = phone.instanceNum,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 状态标签
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // 配置信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val cpuVal = phone.cpMode?.cpu ?: phone.cpu
                val memVal = phone.cpMode?.memory ?: phone.memory
                val dpiVal = phone.cpMode?.dpi ?: "1080"
                InfoChip(Icons.Default.Memory, "CPU", "${cpuVal}核")
                InfoChip(Icons.Default.Storage, "内存", "${memVal / 1024}GB")
                InfoChip(Icons.Default.AspectRatio, "分辨率", dpiVal)
            }

            // 到期时间
            if (phone.endTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "到期: ${phone.endTime}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 连接按钮 - 渐变效果
                Button(
                    onClick = { onConnect(phone) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOnline) CloudColors.Primary else CloudColors.StatusPending
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        if (isOnline) Icons.Default.CellWiFi else Icons.Default.SignalCellularConnectedNoInternet0,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isOnline) "连接" else "已离线", fontSize = 13.sp)
                }

                // 电源菜单
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isOnline) CloudColors.StatusError else CloudColors.StatusOnline
                        )
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("电源", fontSize = 13.sp)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!isOnline) {
                            DropdownMenuItem(
                                text = { Text("开机") },
                                leadingIcon = {
                                    Icon(Icons.Default.Power, null, tint = CloudColors.StatusOnline)
                                },
                                onClick = { showMenu = false; onStart() }
                            )
                        }
                        if (isOnline) {
                            DropdownMenuItem(
                                text = { Text("关机") },
                                leadingIcon = {
                                    Icon(Icons.Default.Stop, null, tint = CloudColors.StatusError)
                                },
                                onClick = { showMenu = false; showConfirmDialog = "stop" }
                            )
                            DropdownMenuItem(
                                text = { Text("重启") },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, null, tint = CloudColors.StatusPending)
                                },
                                onClick = { showMenu = false; showConfirmDialog = "reboot" }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("改名") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; showRenameDialog = true }
                        )
                    }
                }
            }
        }
    }

    // 确认弹窗
    showConfirmDialog?.let { action ->
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text("确认操作") },
            text = {
                Text(
                    when (action) {
                        "stop" -> "确定关闭「${phone.name}」？"
                        "reboot" -> "确定重启「${phone.name}」？"
                        else -> ""
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            "stop" -> onStop()
                            "reboot" -> onReboot()
                        }
                        showConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CloudColors.StatusError)
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) { Text("取消") }
            }
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            currentName = phone.name,
            onRename = { onRename(it); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = CloudColors.Primary)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("修改名称") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("新名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onRename(text) },
                enabled = text.isNotBlank()
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
