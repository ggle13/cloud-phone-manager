package com.example.cloudphone.ui.token

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cloudphone.data.model.TokenEntry
import com.example.cloudphone.ui.theme.CloudColors
import com.example.cloudphone.utils.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenManagerScreen(
    navController: NavController,
    tokenManager: TokenManager,
    onHasToken: () -> Unit
) {
    val viewModel: TokenViewModel = viewModel(
        factory = TokenViewModelFactory(tokenManager)
    )
    val tokens by viewModel.tokens.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(tokens) {
        val active = tokens.find { it.isActive }
        if (active != null) {
            onHasToken()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📱 云机管理", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CloudColors.CloudBlue.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔑 Token 配置说明", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. 打开uphone.wo-adv.cn登录\n" +
                        "2. 按 F12 → Application → Local Storage\n" +
                        "3. 找到 Authorization 或 token 字段\n" +
                        "4. userId 自动从 Token 解析，无需手动填写",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        lineHeight = 20.sp
                    )
                }
            }

            if (tokens.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔑", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暂无 Token", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "点击下方按钮添加",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tokens, key = { it.id }) { token ->
                        TokenCard(
                            token = token,
                            onActivate = { viewModel.activateToken(token.id) },
                            onDelete = { viewModel.deleteToken(token.id) },
                            onRename = { viewModel.renameToken(token.id, it) }
                        )
                    }
                }
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CloudColors.CloudBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加 Token", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (showAddDialog) {
        AddTokenDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { t, r ->
                viewModel.addToken(t, r)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TokenCard(
    token: TokenEntry,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var remark by remember(token.remark) { mutableStateOf(token.remark) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (token.isActive)
                CloudColors.CloudBlue.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (token.isActive) CloudColors.CloudBlue
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = token.remark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (token.isActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CloudColors.CloudBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "✅ 当前",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = CloudColors.CloudBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "展开"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ) {
                Text(
                    text = "${token.token.take(10)}...${token.token.takeLast(6)}",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = remark,
                onValueChange = {
                    remark = it
                    onRename(it)
                },
                label = { Text("备注") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row {
                            Text("UserId：", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(token.userId, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text("DeviceId：", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(token.deviceId.takeLast(8) + "...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            if (!token.isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onActivate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("切换为此账号")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(16.dp),
            title = { Text("删除 Token") },
            text = { Text("确定删除「${token.remark}」吗？删除后需重新添加。") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun AddTokenDialog(
    onDismiss: () -> Unit,
    onAdd: (token: String, remark: String) -> Unit
) {
    var token by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("添加 Token") },
        text = {
            Column {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token（必填）") },
                    placeholder = { Text("粘贴 Authorization 值") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注（必填）") },
                    placeholder = { Text("如：账号A / 个人 / 工作") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (token.isNotBlank() && remark.isNotBlank()) {
                        onAdd(token.trim(), remark.trim())
                    }
                },
                enabled = token.isNotBlank() && remark.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
