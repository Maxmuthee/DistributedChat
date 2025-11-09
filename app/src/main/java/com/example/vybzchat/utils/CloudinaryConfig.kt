package com.example.vybzchat.utils

import com.cloudinary.android.MediaManager
import android.content.Context

class CloudinaryConfig {
    companion object {
        fun init(context: Context) {
            val config = mapOf(
                "cloud_name" to "ds21a6s3s",
                "api_key" to "478946942311642",
                "api_secret" to "RaEhd_-Lynz_9HF-PPAP3NEyVIg"
            )
            MediaManager.init(context, config)
        }
    }
}