package com.greg.uberclone

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.droidman.ktoasty.KToasty
import com.greg.uberclone.databinding.ActivityMainBinding
import com.greg.uberclone.databinding.ActivitySplashScreenBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        splashTimer()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Timer ----------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun splashTimer(){
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                KToasty.success(this@SplashScreenActivity, "Splash screen run done !",
                    Toast.LENGTH_SHORT).show()
            }
    }
}