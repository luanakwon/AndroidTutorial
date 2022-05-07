package com.onbleep.ringgauge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import android.widget.VideoView

class HowToActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how_to)

        val v: VideoView = findViewById(R.id.videoView)
        val skipButton: Button = findViewById(R.id.skipBtn)
        val replayButton: ImageButton = findViewById(R.id.replayBtn)
        val changeTextHandler = Handler(Looper.getMainLooper())
        val videoLengthInMillis: Long = 30000 // 30s
        // TODO: change video
        v.setVideoPath("android.resource://${packageName}/${R.raw.how_to_0507}")
        v.start()


        skipButton.setOnClickListener {
            val myIntent = Intent(this, InformPermissionActivity::class.java)
            startActivity(myIntent)
        }
        replayButton.setOnClickListener {
            v.start() // replay video
            skipButton.setText("Skip") // change "next" to "skip"
            changeTextHandler.postDelayed({ // set timer
                skipButton.setText("Next") // to change the skipButton text
                replayButton.visibility = View.VISIBLE // and to show replayButton
            },videoLengthInMillis)
            it.visibility = View.GONE // remove replayButton
        }
        changeTextHandler.postDelayed({// set timer
            skipButton.setText("Next")
            replayButton.visibility = View.VISIBLE
        },videoLengthInMillis)
    }

    var mBackWait: Long = 0
    override fun onBackPressed() {
        if (System.currentTimeMillis() - mBackWait >= 2000){
            mBackWait = System.currentTimeMillis()
            Toast.makeText(
                this, "뒤로가기 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT)
                .show()
        } else {
            finishAffinity()
        }
    }
}