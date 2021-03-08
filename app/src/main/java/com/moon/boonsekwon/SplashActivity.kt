package com.moon.boonsekwon

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private var SPLASH_TIME = 3000L //This is 1 seconds
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        //thread for splash screen running
        //thread for splash screen running
        val logoTimer: Thread = object : Thread() {
            override fun run() {
                try {
                    sleep(SPLASH_TIME)
                } catch (e: InterruptedException) {
                    Log.d("Exception", "Exception$e")
                } finally {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                }
                finish()
            }
        }
        logoTimer.start()
    }
}