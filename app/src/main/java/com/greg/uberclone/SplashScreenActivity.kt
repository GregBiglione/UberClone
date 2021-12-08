package com.greg.uberclone

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.greg.uberclone.databinding.ActivityMainBinding
import com.greg.uberclone.databinding.ActivitySplashScreenBinding

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}