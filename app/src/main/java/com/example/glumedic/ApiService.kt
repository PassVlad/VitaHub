package com.example.glumedic

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ---------- Auth ----------
    @POST("api/register/")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @FormUrlEncoded
    @POST("api/login/")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<AuthResponse>

    // ---------- Profile ----------
    @GET("api/profile/")
    suspend fun getProfile(): Response<ProfileResponse>

    @PATCH("api/profile/")
    suspend fun updateProfile(@Query("full_name") fullName: String): Response<ProfileResponse>

    // ---------- Vitals ----------
    @GET("api/metrics/")
    suspend fun getMetrics(): Response<List<MetricDefinition>>

    @GET("api/dashboard/")
    suspend fun getDashboard(): Response<DashboardSummary>

    @GET("api/measurements/")
    suspend fun getMeasurements(
        @Query("metric") metric: String? = null,
        @Query("limit") limit: Int = 200
    ): Response<List<VitalMeasurement>>

    @POST("api/measurements/")
    suspend fun addMeasurement(@Body request: AddMeasurementRequest): Response<VitalMeasurement>

    @GET("api/measurements/stats/")
    suspend fun getStats(@Query("metric") metric: String): Response<VitalStats>

    @GET("api/measurements/{metric}/")
    suspend fun getSeries(
        @Path("metric") metric: String,
        @Query("days") days: Int? = null
    ): Response<List<VitalSeriesPoint>>

    @PATCH("api/measurements/{id}/")
    suspend fun updateMeasurement(
        @Path("id") id: Int,
        @Body request: UpdateMeasurementRequest
    ): Response<VitalMeasurement>

    @DELETE("api/measurements/{id}/")
    suspend fun deleteMeasurement(@Path("id") id: Int): Response<Unit>

    // ---------- Chat / Assistant ----------
    @POST("api/ask/")
    suspend fun ask(@Body request: AskRequest): Response<AskResponse>

    // ---------- Documents ----------
    @GET("api/documents/list/")
    suspend fun listDocuments(): Response<List<DocumentInfo>>

    @POST("api/documents/upload/")
    suspend fun uploadDocument(@Body request: UploadDocumentRequest): Response<DocumentUploadResponse>

    @Multipart
    @POST("api/documents/upload-file/")
    suspend fun uploadDocumentFile(@Part file: MultipartBody.Part): Response<DocumentUploadResponse>

    @DELETE("api/documents/{id}/")
    suspend fun deleteDocument(@Path("id") id: Int): Response<Unit>
}

// ================= МОДЕЛИ (контракт сервера VitaHub) =================

data class MetricDefinition(
    val key: String,
    val name: String = "",
    val short: String = "",
    val unit: String = "",
    val min: Double? = null,
    val max: Double? = null,
    val min2: Double? = null,
    val max2: Double? = null,
    val two_values: Boolean = false,
    val decimals: Int = 1,
    val description: String = "",
    val icon: String = ""
)

data class DashboardSummary(
    val user: ProfileResponse? = null,
    val totals: Map<String, Int> = emptyMap(),
    val metrics: List<MetricDefinition> = emptyList(),
    val latest: Map<String, VitalMeasurement?> = emptyMap()
)

data class VitalMeasurement(
    val id: Int,
    val metric: String,
    val value: Double,
    val value2: Double? = null,
    val unit: String = "",
    val measured_at: String,
    val notes: String? = null,
    val created_at: String? = null
)

data class AddMeasurementRequest(
    val metric: String,
    val value: Double,
    val value2: Double? = null,
    val measured_at: String? = null,
    val notes: String? = null
)

data class UpdateMeasurementRequest(
    val value: Double? = null,
    val value2: Double? = null,
    val measured_at: String? = null,
    val notes: String? = null
)

data class VitalStats(
    val metric: String,
    val unit: String = "",
    val count: Int = 0,
    val latest: Double? = null,
    val latest2: Double? = null,
    val average: Double? = null,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val measured_at: String? = null,
    val in_range_pct: Double? = null,
    val status: String = "n/a"
)

data class VitalSeriesPoint(
    val id: Int,
    val value: Double,
    val value2: Double? = null,
    val measured_at: String,
    val notes: String? = null
)

data class AskRequest(
    val query: String,
    val use_internet: Boolean = false
)

data class AskResponse(
    val answer: String,
    val source: String = "general",
    val chat_id: Int = 0,
    val created_at: String? = null
)

data class DocumentInfo(
    val id: Int,
    val filename: String,
    val file_size: Long? = null,
    val chunks_count: Int? = null,
    val uploaded_at: String? = null,
    val has_text: Boolean = false,
    val preview: String? = null
)

data class UploadDocumentRequest(
    val text: String,
    val filename: String = "document.txt"
)

data class DocumentUploadResponse(
    val status: String,
    val chunks_added: Int = 0,
    val user_id: Int? = null,
    val document_id: Int? = null
)
