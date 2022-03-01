package com.mediapipe.camera2kotlin

// Based on TomerPacific's
// github -> https://github.com/TomerPacific/MediumArticles/blob/master/Camrea2API/app/src/main/java/com/tomerpacific/camera2api/MainActivity.kt
// medium ->
// freecodecamp ->
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.Window
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.IllegalStateException

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity: "
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

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
    private lateinit var videoSize: Size


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
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
            if (allPermissionsGranted()){
                Log.i(TAG,"Permissions granted (onRequestPermissionsResult)")
            } else {
                Log.e(TAG, "Permissions denied by the user (onRequestPermissionsResult)")
                this.finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        // check permissions
        if (allPermissionsGranted()){
            Log.i(TAG, "Permissions already granted (onCreate)")
        } else {
            Log.i(TAG, "Requesting for permissions (onCreate)")
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // lateinit Views
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // start thread
        startBackgroundThread()


        // Camera Manager and CameraId
//        val cameraManager : CameraManager =  getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        val cameraIds: Array<String> = cameraManager.cameraIdList
//        var cameraId: String = ""
//        for (id in cameraIds){
//            val cameraConfig = cameraManager.getCameraCharacteristics(id)
//            // choose lens facing back camera
//            if (cameraConfig.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK){
//                continue
//            }
//
////            val previewSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
////            val imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
////            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
//            cameraId = id
//        }

    }
    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        startBackgroundThread() // TODO: should I check if the thread already exists?
        if (textureView.isAvailable){
            setupCamera()
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    private fun setupCamera(){
        val cameraIds: Array<String> = cameraManager.cameraIdList

        for (id in cameraIds){
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            // choose lens facing back camera
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK){
                continue
            }

            // i think !!. operator would be enough
            previewSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
            //previewSize = Size(1280,720)
            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)

            cameraId = id
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectCamera(){
        cameraManager.openCamera(cameraId, cameraStateCallback,backgroundHandler)
    }

    // Surface texture listener
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
            if (allPermissionsGranted()){
                setupCamera()
                connectCamera()
                println("$p1, $p2, ${previewSize.width}, ${previewSize.height}")
            }
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            //TODO("Not yet implemented")
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
            //TODO("Not yet implemented")
            return true
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            //TODO("Not yet implemented")
        }
    }

    // camera State Callback
    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            val surfaceTexture: SurfaceTexture? = textureView.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(previewSize.width,previewSize.height)
            val previewSurface: Surface = Surface(surfaceTexture)

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(previewSurface)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sessionConfig: SessionConfiguration =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(OutputConfiguration(previewSurface), OutputConfiguration(imageReader.surface)),
                        ContextCompat.getMainExecutor(this@MainActivity),
                        captureStateCallback
                    )
                cameraDevice.createCaptureSession(sessionConfig)
            } else { // this is deprecated since api level 30
                cameraDevice.createCaptureSession(
                    listOf(previewSurface,imageReader.surface)
                    ,captureStateCallback
                    ,null)
            }
        }

        override fun onDisconnected(p0: CameraDevice) {
            //TODO("Not yet implemented")
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            val errorMsg = when(error) {
                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                ERROR_CAMERA_DISABLED -> "Device policy"
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                else -> "Unknown"
            }
            Log.e(TAG, "Error when trying to connect camera $errorMsg")
        }
    }

    // Handler
    private fun startBackgroundThread(){
        backgroundHandlerThread = HandlerThread("CameraVideoThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(
            backgroundHandlerThread.looper
        )
    }
    private fun stopBackgroundThread(){
        backgroundHandlerThread.quitSafely()
        backgroundHandlerThread.join()
    }

    // CaptureStateCallback
    private val captureStateCallback = object : CameraCaptureSession.StateCallback(){
        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            try {
                cameraCaptureSession.setRepeatingRequest(
                    captureRequestBuilder.build(), null,
                    backgroundHandler
                )
                // is this only for mediaRecorder?
            } catch (e: CameraAccessException){
                e.printStackTrace()
                Log.e(TAG, "Failed to start cam preview. Couldn't access the camera")
            } catch (e: IllegalStateException){
                e.printStackTrace() // ..?
            }
        }

        override fun onConfigureFailed(p0: CameraCaptureSession) {
            Log.e(TAG, "captureStateCallback Configuration failed")
        }
    }

    // Image reader -- is this for taking photo?
    val onImageAvailableListener = object : ImageReader.OnImageAvailableListener{
        override fun onImageAvailable(reader: ImageReader) {
            Toast.makeText(this@MainActivity, "Photo Taken!!", Toast.LENGTH_SHORT).show()
            val image: Image = reader.acquireLatestImage()
            image.close()
        }
    }
}