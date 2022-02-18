package com.example.mykotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.opengl.Visibility
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.*
import org.opencv.core.Mat

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    companion object {
        private const val TAG = "MainActivity: "
        private const val REQUEST_CODE_PERMISSION = 10
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    private var cameraBridgeViewBase: CameraBridgeViewBase? = null
    private val baseLoaderCallback = object : BaseLoaderCallback(this){
        override fun onManagerConnected(status: Int) {
            when (status){
                LoaderCallbackInterface.SUCCESS ->{
                    Log.e(TAG, "onManagerConnected: Opencv loaded")
                    System.loadLibrary("native-lib")
                    cameraBridgeViewBase?.enableView()
                }
                else -> super.onManagerConnected(status)
            }

        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSION.all{
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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

        if (allPermissionsGranted()){
            Log.i(TAG, "Permissions granted")
            //cameraBridgeViewBase?.setCameraPermissionGranted doesn't exist
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSION, REQUEST_CODE_PERMISSION)
        }

        cameraBridgeViewBase = findViewById(R.id.camView)
        cameraBridgeViewBase?.visibility = SurfaceView.VISIBLE
        cameraBridgeViewBase?.setCvCameraViewListener(this)


//        val CDD = CardDetector(mk.ndarray(mk[170,140]),mk.ndarray(mk[160,200]),0.2f)
//        var textView = findViewById<TextView>(R.id.textView)
//        textView.text = CDD.area.toString()

//        val a = Mat.zeros(1,4,CvType.CV_8U)
//        val b = Mat()
//        a.put(0,0,1.0)
//        a.put(0,1,3.0)
//        a.put(0,2,5.0)
//        a.put(0,3,7.0)
//        Core.repeat(a,3,1,b)
//        println(a)
//        println(b)
//        Core.subtract(b, Scalar(2.0), b)
//        for(i in 0..2){
//            for(j in 0..3){
//                println("$i, $j, ${b.get(i,j)[0]}")
//            }
//        }



    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onCameraViewStopped() {
        TODO("Not yet implemented")
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        TODO("Not yet implemented")
    }

}