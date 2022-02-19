package com.example.mykotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2{

    companion object {
        private const val TAG = "MainActivity: "
        private const val REQUEST_CODE_PERMISSION = 1001
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    private var cameraBridgeViewBase : CameraBridgeViewBase? = null
    private var baseLoaderCallback : BaseLoaderCallback? = null
    //private var mat : Mat = Mat()

    private fun allPermissionsGranted() = REQUIRED_PERMISSION.all{
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
        if(requestCode == REQUEST_CODE_PERMISSION){
            if (allPermissionsGranted()){
                Log.i(TAG, "Permissions granted")
            } else {
                Toast.makeText(
                    this, "Permissions denied by the user", Toast.LENGTH_SHORT).show()
                this.finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // check if permissions are granted
        if (allPermissionsGranted()){
            Log.i(TAG, "Permissions granted")
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSION, REQUEST_CODE_PERMISSION
            )
        }

        cameraBridgeViewBase = findViewById(R.id.myCamView)
        cameraBridgeViewBase!!.visibility = SurfaceView.VISIBLE
        cameraBridgeViewBase!!.setCvCameraViewListener(this)
        baseLoaderCallback = object : BaseLoaderCallback(this){
            override fun onManagerConnected(status: Int) {
                when (status){
                    SUCCESS -> cameraBridgeViewBase!!.enableView()
                    else -> super.onManagerConnected(status)
                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
        cameraBridgeViewBase?.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraBridgeViewBase?.disableView()
    }

    override fun onResume() {
        super.onResume()
        if(!OpenCVLoader.initDebug()){
            Log.e(TAG, "opencv init debug error")
        } else {
            baseLoaderCallback?.onManagerConnected(BaseLoaderCallback.SUCCESS)
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        //mat = Mat(width, height, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        //TODO "Not yet implemented"
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val mRGBA = inputFrame?.gray()

        return mRGBA ?: Mat()
    }
}