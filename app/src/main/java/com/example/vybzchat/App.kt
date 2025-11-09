package com.example.vybzchat

import android.app.Application
import com.google.firebase.FirebaseApp
import com.example.vybzchat.utils.CloudinaryConfig
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        CloudinaryConfig.init(this)
    }
}
