package com.onbleep.ringgauge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class InformPermissionActivity : AppCompatActivity() {

    companion object {
        private val TAG = "InformPermissionActivity"
    }

    private var alreadyCreated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inform_permission)
        alreadyCreated = true

        if (allPermissionsGranted()){
            val myIntent = Intent(this, MeasureActivity::class.java)
            startActivity(myIntent)
        }

        findViewById<Button>(R.id.reqPermBtn).setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                MeasureActivity.REQUIRED_PERMISSION,
                MeasureActivity.REQUEST_CODE_PERMISSION
            )
        }
    }

    private fun allPermissionsGranted() = MeasureActivity.REQUIRED_PERMISSION.all{
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Idk but once I remove this super it gives me an error
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == MeasureActivity.REQUEST_CODE_PERMISSION){
            if (allPermissionsGranted()){
                Log.i(TAG, "(onReqPerm) Permissions granted")
                val myIntent = Intent(this, MeasureActivity::class.java)
                startActivity(myIntent)
            } else {
                Toast.makeText(
                    this, "Permissions denied by the user", Toast.LENGTH_SHORT).show()
                //this.finish()
            }
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
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        if (alreadyCreated && allPermissionsGranted()){
            val myIntent = Intent(this, MeasureActivity::class.java)
            startActivity(myIntent)
        }
    }
}