package com.example.cloudphone.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class AuthInterceptor(
    private val tokenProvider: () -> String,
    private val deviceIdProvider: () -> String = { "" }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()
        val deviceId = deviceIdProvider().takeIf { it.isNotEmpty() }
            ?: "16f5fc83-c08b-438f-8cb0-46eea5123931"
        val timestamp = System.currentTimeMillis().toString()

        val builder = original.newBuilder()
            // App 版完整请求头（来自真实抓包）
            .header("Authorization", token)
            .header("internationalFlag", "zh")
            .header("channel", "360A")
            .header("source", "1")
            .header("version", "2.3.0")
            .header("deviceId", deviceId)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("timestamp", timestamp)
            .header("Accept-Encoding", "gzip")
            .header("User-Agent", "okhttp/4.12.0")

        return chain.proceed(builder.build())
    }
}
