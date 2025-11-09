package com.example.vybzchat.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ImageUploader {

    fun uploadImage(context: Context, imageUri: Uri): Flow<UploadResult> = callbackFlow {
        val requestId = MediaManager.get().upload(imageUri)
            .option("folder", "vybz_chat")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    trySend(UploadResult.Loading)
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = if (totalBytes > 0) (bytes * 100 / totalBytes).toInt() else 0
                    trySend(UploadResult.Progress(progress))
                }

                override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                    val imageUrl = resultData["secure_url"] as? String
                    if (imageUrl != null) {
                        trySend(UploadResult.Success(imageUrl))
                    } else {
                        trySend(UploadResult.Error("Failed to get image URL"))
                    }
                    close()
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    trySend(UploadResult.Error(error.description ?: "Unknown error"))
                    close()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    trySend(UploadResult.Error("Upload rescheduled: ${error.description}"))
                }
            })
            .dispatch()

        awaitClose {
            // Cleanup if needed when the flow is cancelled
        }
    }

    sealed class UploadResult {
        object Loading : UploadResult()
        data class Progress(val percentage: Int) : UploadResult()
        data class Success(val imageUrl: String) : UploadResult()
        data class Error(val message: String) : UploadResult()
    }
}