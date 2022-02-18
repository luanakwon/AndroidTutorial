package com.mediapipe.eventhandle_mphand

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.solutioncore.CameraInput
import com.google.mediapipe.solutioncore.ResultGlRenderer
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView
import com.google.mediapipe.solutions.hands.HandLandmark
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import android.R

import android.widget.FrameLayout




class MainActivity_gl: AppCompatActivity() {

    private var noResult = 0
    private var yesResult = 0
    private var reqResult = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.)

        // Request camera permission
        if (!allPermissionsGranted()){
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        val handsOptions = HandsOptions.builder()
            .setStaticImageMode(false)
            .setMaxNumHands(1)
            .setRunOnGpu(false).build()

        val hands = Hands(this, handsOptions)

        val cameraInput = CameraInput(this)

        hands.setErrorListener {
                message, e -> Log.e(TAG, "mp hands error $message") }
        cameraInput.setNewFrameListener {
            it -> hands.send(it)
            reqResult += 1
        }

        val glSurfaceView = SolutionGlSurfaceView<HandsResult>(
            this, hands.glContext, hands.glMajorVersion
        )
        glSurfaceView.setSolutionResultRenderer(HandsResultGlRenderer())
        glSurfaceView.setRenderInputImage(true)

        hands.setResultListener {
            result ->
                if (result.multiHandLandmarks().isEmpty()){
                    noResult += 1
                } else {
                    val wristLandmark =
                        result.multiHandLandmarks()[0].landmarkList[HandLandmark.WRIST]
                    Log.i(
                        TAG,
                        "MP hand wrist normalized coordinates(range [0,1]):" +
                                "${wristLandmark.x} ${wristLandmark.y}"
                    )
                    // request gl rendering
                    glSurfaceView.setRenderData(result)
                    glSurfaceView.requestRender()
                }
        }

        glSurfaceView.post {
            cameraInput.start(
                this,
                hands.glContext,
                CameraInput.CameraFacing.BACK,
                glSurfaceView.width,
                glSurfaceView.height
            )
        }

        // Updates the preview layout.
        val frameLayout = findViewById(R.id.preview)
        frameLayout.removeAllViewsInLayout()
        frameLayout.addView(glSurfaceView)
        glSurfaceView.visibility = View.VISIBLE
        frameLayout.requestLayout()
    }

    private fun allPermissionsGranted() = Companion.REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (!allPermissionsGranted()){
                Toast.makeText(
                    this, "Permissions denied by the user", Toast.LENGTH_SHORT).show()
                this.finish()
            }
        }
    }

    companion object{
        private val TAG = "MainActivity_gl"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).toTypedArray()
    }

}