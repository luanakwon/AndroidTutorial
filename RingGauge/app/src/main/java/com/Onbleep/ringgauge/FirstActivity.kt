package com.Onbleep.ringgauge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class FirstActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        Handler().postDelayed({
            val intent = Intent(this, HowToActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            finish()
        },DURATION)
    }
    companion object {
        private const val DURATION: Long = 3000
    }
    // Is this necessary?
    override fun onBackPressed(){
        super.onBackPressed()
    }

}