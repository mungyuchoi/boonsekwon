package com.moon.boonsekwon.register

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.moon.boonsekwon.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Log.i("MQ!", "latitude: ${intent.getStringExtra("latitude")}, longitude:${intent.getStringExtra("longitude")}")
    }
}