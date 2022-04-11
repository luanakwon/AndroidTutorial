package com.Onbleep.ringgauge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.VideoView

class HowToActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how_to)

        val v: VideoView = findViewById(R.id.videoView)
        // TODO: change video
        v.setVideoPath("android.resource://com.Onbleep.RingGauge/"+R.raw.testvid)
        v.start()

        findViewById<Button>(R.id.skipBtn).setOnClickListener {
            val myIntent = Intent(this, InformPermissionActivity::class.java)
            startActivity(myIntent)
        }
    }

    var mBackWait: Long = 0

    override fun onBackPressed() {
        if (System.currentTimeMillis() - mBackWait >= 2000){
            mBackWait = System.currentTimeMillis()
            Toast.makeText(
                this, "뒤로가기 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT)
                .show()
        } else {
            finish()
        }
    }
}