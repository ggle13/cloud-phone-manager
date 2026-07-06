package com.example.cloudphone.data.model

import com.google.gson.annotations.SerializedName

data class CloudPhone(
    // 基础信息
    val id: Int = 0,
    @SerializedName("instanceNum") val instanceNum: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("endTime") val endTime: String = "",
    @SerializedName("startTime") val startTime: String? = null,

    // WebRTC 信令需要
    @SerializedName("remoteName") val remoteName: String? = null,

    // 资源配置
    @SerializedName("cpu") val cpu: Int = 0,
    @SerializedName("memory") val memory: Int = 0,
    @SerializedName("disk") val disk: Int = 0,

    // 地域
    @SerializedName("regionId") val regionId: Int = 0,
    @SerializedName("regionUuid") val regionUuid: String? = null,

    // 用户
    @SerializedName("userId") val userId: String? = null,

    // 订单相关
    @SerializedName("orderId") val orderId: Long? = null,
    @SerializedName("tradeCode") val tradeCode: String? = null,
    @SerializedName("cpId") val cpId: Int? = null,
    @SerializedName("cpUuid") val cpUuid: String? = null,
    @SerializedName("cpImageUuid") val cpImageUuid: String? = null,
    @SerializedName("cpDiskUuid") val cpDiskUuid: String? = null,
    @SerializedName("cpModeUuid") val cpModeUuid: String? = null,
    @SerializedName("cpModelUuid") val cpModelUuid: String? = null,
    @SerializedName("snapShootImg") val snapShootImg: String? = null,
    @SerializedName("limitedTimeStatus") val limitedTimeStatus: String? = null,

    // H5 API 字段
    @SerializedName("cpMode") val cpMode: CpMode? = null
)

// H5 API 的 cpModel/cpMode 嵌套对象
data class CpMode(
    @SerializedName("cpu") val cpu: Int = 0,
    @SerializedName("memory") val memory: Int = 0,
    @SerializedName("disk") val disk: Int = 0,
    @SerializedName("dpi") val dpi: String? = null
)

data class ApiResponse<T>(
    val code: Int,
    val msg: String?,
    val data: T?,
    val seq: String? = null,
    val total: Int? = null,
    val rows: List<CloudPhone>? = null
)

// 云机媒体服务器信息（来自 signalAddress API 或 WebSocket response）
data class SignalAddress(
    @SerializedName("instanceId") val instanceId: String? = null,
    @SerializedName("peerId") val peerId: String? = null,
    @SerializedName("signallingAddress") val signallingAddress: String? = null,
    @SerializedName("mediaAddress") val mediaAddress: String? = null,
    @SerializedName("outterIp") val outterIp: String? = null,
    @SerializedName("outterPort4") val outterPort4: Int? = null,
    @SerializedName("cphWidth") val cphWidth: Int? = null,
    @SerializedName("cphHeight") val cphHeight: Int? = null,
    @SerializedName("cphFps") val cphFps: Int? = null,
    @SerializedName("supportNewTouchCmd") val supportNewTouchCmd: Boolean? = null,
    @SerializedName("touchVersion") val touchVersion: Int? = null
)
