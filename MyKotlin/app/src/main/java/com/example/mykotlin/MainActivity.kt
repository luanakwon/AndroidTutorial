package com.example.mykotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceView
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2{

    companion object {
        private const val TAG = "MainActivity: "
        private const val REQUEST_CODE_PERMISSION = 1001
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    // opencv baseLoaderCallback..?
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

    private lateinit var cornersDst : Array<Point>
    private lateinit var cardDetector: CardDetector
    private lateinit var textureView: TextureView
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var cameraId: String
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var previewSize: Size

    private var captureSessionOccupied: Boolean = false
    private var connectCameraFirstCall: Boolean = true

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
        supportActionBar?.hide()
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

        // init opencv
        OpenCVLoader.initDebug()

        // lateinit views
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // capture btn onClickListener
        findViewById<Button>(R.id.captureBtn).setOnClickListener {captureSurface()}

        // start cameraVideoThread
        startBackgroundThread()

        // initialize card detector
        cardDetector = CardDetector(
            mk.ndarray(mk[170,140]), mk.ndarray(mk[160,200]),0.2f)

        // array of point to get detected corners of card
        cornersDst = Array<Point>(4){ Point(0.0,0.0) } //TODO -> done..?
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy called")
        cameraDevice.close()
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        //TODO From here https://github.com/luanakwon/AndroidTutorial/blob/main/Camera2Kotlin/app/src/main/java/com/mediapipe/camera2kotlin/MainActivity.kt
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
            val resultPair = cardDetector!!.run(it, )
            Log.i(TAG, "card detected : ${resultPair.first}")
        }
        return mRGBA ?: Mat()
    }
}