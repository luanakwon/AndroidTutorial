// Tried mediapipe implementation with camerax
// got stuck in the imageProxy -> bitmap conversion

//package com.mediapipe.eventhandle_mphand
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.nfc.Tag
//import android.os.Build
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.google.mediapipe.framework.TextureFrame
//import com.google.mediapipe.solutions.hands.Hands
//import com.google.mediapipe.solutions.hands.HandsOptions
//import java.lang.Exception
//
//class MainActivity : AppCompatActivity() {
//
//    // camera viewFinder
//    private var previewView: PreviewView? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        previewView = findViewById(R.id.prvView)
//        hands = Hands(this, handsOptions)
//        hands?.setErrorListener {
//                message, e -> Log.e(TAG,"Mediapipe hands error: $message") }
//
//        // Request camera permissions
//        if (allPermissionsGranted()) {
//            startCamera()
//        } else {
//            ActivityCompat.requestPermissions(
//                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
//        }
//    }
//
//    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
//        ContextCompat.checkSelfPermission(
//            baseContext, it)== PackageManager.PERMISSION_GRANTED
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CODE_PERMISSIONS){
//            if(allPermissionsGranted()){
//                startCamera()
//            } else {
//                Toast.makeText(this,"Permissions not granted by the user",Toast.LENGTH_SHORT).show()
//                this.finish()
//            }
//        }
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            // bind the lifecycle of cameras to the lifecycle owner
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//            // preview
//            val preview = Preview.Builder()
//                .build()
//                .also {
//                    it.setSurfaceProvider(previewView?.surfaceProvider)
//                }
//
//            // select back camera as the default
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            try {
//                // unbind all use cases before binding
//                cameraProvider.unbindAll()
//                // bind use cases to camera
//                cameraProvider.bindToLifecycle(
//                    this,cameraSelector,preview)
//            } catch (exc: Exception){
//                Log.e(TAG, "Use cases binding failed", exc)
//            }
//        }, ContextCompat.getMainExecutor(this))
//
//    }
//
//    private class MpHandAnalyzer: ImageAnalysis.Analyzer {
//        override fun analyze(image: ImageProxy) {
//            TODO("convert image proxy to bitmap or textureFrame and send it to mediapipe hands")
//
//        }
//    }
//
//    companion object {
//        private const val TAG = "MyActivity "
//        private const val REQUEST_CODE_PERMISSIONS = 10
//        private val REQUIRED_PERMISSIONS =
//            mutableListOf (
//                Manifest.permission.CAMERA
//            ).toTypedArray()
//
//        // mediapipe hand configuration
//        private val handsOptions =
//            HandsOptions.builder()
//                .setStaticImageMode(false)
//                .setMaxNumHands(1)
//                .setRunOnGpu(false)
//                .build()
//        // mediapipe hand detection model
//        var hands: Hands?= null
//    }
//}
//
