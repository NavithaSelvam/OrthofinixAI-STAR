package com.example.orthofinixai.data.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

data class UploadResponse(
    val upload_id: String,
    val image_url: String
)

data class BackendAnalysisResponse(
    val id: String,
    val user_id: String,
    val patient_name: String,
    val image_url: String,
    val view_type: String,
    val status: String,
    val finishing_score: Float,
    val alignment_score: Float,
    val confidence_score: Float,
    val midline_deviation_mm: Float,
    val overjet_mm: Float,
    val overbite_percent: Float,
    val abo_score: Float,
    val andrews_score: Float,
    val prediction: String,
    val recommendations: List<String>,
    val metrics: Map<String, Any>,
    val created_at: String?,
    val root_angulation_score: Float = 0f
)

interface OrthofinixApi {

    @Multipart
    @POST("analysis/upload")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): UploadResponse

    @FormUrlEncoded
    @POST("analysis/analyze")
    suspend fun analyzeImage(
        @Header("Authorization") token: String,
        @Field("upload_id") uploadId: String,
        @Field("patient_name") patientName: String,
        @Field("view_type") viewType: String
    ): BackendAnalysisResponse

    companion object {

        private val BASE_URL = ApiConfig.BASE_URL

        @Volatile
        private var INSTANCE: OrthofinixApi? = null

        fun create(): OrthofinixApi {
            return INSTANCE ?: synchronized(this) {

                val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(createOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val instance = retrofit.create(OrthofinixApi::class.java)

                INSTANCE = instance
                instance
            }
        }

        private fun createOkHttpClient(): OkHttpClient {

            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            return OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}