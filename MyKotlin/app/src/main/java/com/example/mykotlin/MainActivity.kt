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
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
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
    private var baseLoaderCallback : BaseLoaderCallback? = object : BaseLoaderCallback(this){
        override fun onManagerConnected(status: Int) {
            when (status){
                SUCCESS -> {
                    Log.i(TAG, "opencv loaded successfully")
                    //maybe??
                    cameraBridgeViewBase!!.enableView()
                }
                else -> super.onManagerConnected(status)
            }
        }
    }
    //private var mat : Mat = Mat()
    private var cardDetector: CardDetector? = null

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
                Log.i(TAG, "(onReqPerm) Permissions granted")
            } else {
                Toast.makeText(
                    this, "(onReqPerm) Permissions denied by the user", Toast.LENGTH_SHORT).show()
                this.finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // check if permissions are granted
        if (allPermissionsGranted()){
            Log.i(TAG, "(onCreate) Permissions granted")
        } else {
            Log.i(TAG, "(onCreate) requested permission")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSION, REQUEST_CODE_PERMISSION
            )
        }

        OpenCVLoader.initDebug()

        // create camera surface
        cameraBridgeViewBase = findViewById(R.id.myCamView)
        cameraBridgeViewBase!!.visibility = SurfaceView.VISIBLE
        cameraBridgeViewBase!!.setCvCameraViewListener(this)


        // initialize card detector
        Log.i(TAG, "initialize card detector")
        cardDetector = CardDetector(
            mk.ndarray(mk[170,140]), mk.ndarray(mk[160,200]),0.2f)
        Log.i(TAG, "initialized card detector")
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
        val mRGBA = inputFrame?.rgba()
        inputFrame?.gray()?.let {
            Log.i(TAG, "inputframe gray")
            val resultPair = cardDetector!!.run(it)
            Log.i(TAG, "card detected : ${resultPair.first}")
        }
        return mRGBA ?: Mat()
    }
}