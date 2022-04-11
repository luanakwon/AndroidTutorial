package com.Onbleep.ringgauge

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MeasureActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity: "
        const val REQUEST_CODE_PERMISSION = 1001
        val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measure)
    }
}