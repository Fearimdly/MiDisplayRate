package com.moki.midisplayrate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.moki.midisplayrate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        startService(Intent(this, FloatWindowService::class.java))

        binding.showFloatWindow.setOnClickListener {
            FloatWindowService.showFloatWindow(this)
        }
    }
}