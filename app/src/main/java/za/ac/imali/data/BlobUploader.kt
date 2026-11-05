package za.ac.imali.data

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object BlobUploader {
    suspend fun uploadReceipt(token: String, file: File, contentType: String): String? {
        val client = OkHttpClient()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(contentType.toMediaTypeOrNull()),
            )
            .build()
        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}api/blobs")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = response.body?.string() ?: return null
            val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
            return id?.let { "${ApiClient.BASE_URL}api/blobs/$it" }
        }
    }
}
