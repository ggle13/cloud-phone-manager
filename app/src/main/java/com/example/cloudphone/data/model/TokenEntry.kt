package com.example.cloudphone.data.model

import com.google.gson.annotations.SerializedName

data class TokenEntry(
    val id: String,
    val token: String,
    val userId: String,    // 自动从 JWT 解析
    val deviceId: String,  // 自动生成/存储
    val remark: String = ""
)
