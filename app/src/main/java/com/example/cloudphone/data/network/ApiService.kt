package com.example.cloudphone.data.network

import com.example.cloudphone.data.model.*
import retrofit2.http.*

interface ApiService {
    // 云机列表 - App API（ext/list/ext）
    @GET("bucp/servers/resource/instance/ext/list/ext")
    suspend fun getCloudPhoneList(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 1000
    ): ApiResponse<List<CloudPhone>>

    // 云机列表 - H5 API（/list）
    @GET("bucp/servers/resource/instance/list")
    suspend fun getCloudPhoneListH5(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 1000
    ): ApiResponse<List<CloudPhone>>

    // 改名
    @PUT("bucp/servers/resource/instance")
    suspend fun renameInstance(@Body request: RenameRequest): ApiResponse<Any>

    // 同步状态
    @GET("bucp/servers/resource/instance/syncStatus")
    suspend fun getInstanceStatus(@Query("cpInstanceId") instanceId: Int): ApiResponse<CloudPhone>

    // 开机
    @POST("bucp/servers/resource/instance/start")
    suspend fun startInstance(@Query("cpInstanceId") instanceId: Int): ApiResponse<Any>

    // 关机
    @POST("bucp/servers/resource/instance/stop")
    suspend fun stopInstance(@Query("cpInstanceId") instanceId: Int): ApiResponse<Any>

    // 重启
    @POST("bucp/servers/resource/instance/restart")
    suspend fun restartInstance(@Query("cpInstanceId") instanceId: Int): ApiResponse<Any>

    // 获取云机媒体信息（信号地址）
    @GET("bucp/servers/resource/instance/signal/address")
    suspend fun getSignalAddress(@Query("cpInstanceId") instanceId: Int): ApiResponse<SignalAddress>

    // 获取用户信息
    @GET("bucp/servers/system/user/getAppUserInfo")
    suspend fun getAppUserInfo(): ApiResponse<UserInfo>

    // 云机已装应用列表
    @GET("bucp/servers/resource/app/installed-info")
    suspend fun getInstalledApps(@Query("cpInstanceId") instanceId: Int): ApiResponse<List<AppInfo>>

    // 云机磁盘/内存信息
    @GET("bucp/servers/resource/instance/ext/disk-memory")
    suspend fun getDiskMemory(@Query("id") instanceId: Int): ApiResponse<DiskMemory>
}

// ─── 请求/响应数据类 ───
data class RenameRequest(
    val id: String,
    val name: String,
    @Transient val noStreamDuration: Any? = null  // @Transient 不参与序列化
)

data class UserInfo(
    val userId: String,
    val userName: String,
    val nickName: String,
    val phonenumber: String
)

data class AppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val iconUrl: String?
)

data class DiskMemory(
    val totalMb: Int,
    val usedMb: Int
)
