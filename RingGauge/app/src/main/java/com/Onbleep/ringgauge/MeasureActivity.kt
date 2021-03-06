package com.onbleep.ringgauge

import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.provider.ContactsContract
import android.util.Size
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

class MeasureActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity: "
        const val REQUEST_CODE_PERMISSION = 1001
        val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    // opencv baseLoaderCallback..?
    private var baseLoaderCallback: BaseLoaderCallback? = object: BaseLoaderCallback(this){
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i(TAG, "opencv loaded successfully")
                }
                else -> super.onManagerConnected(status)
            }
        }
    }

    private lateinit var cornersDst : Array<Point>
    private lateinit var cardDetector: CardDetector
    private lateinit var fingerDipDetector: FingerDipDetection
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var cameraId: String
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var previewSize: Size

    private lateinit var grImg: Mat
    private lateinit var rgbImg: Mat
    private lateinit var cornerGuides: Array<ImageView>
    private lateinit var corner1stDots: Array<RelativeLayout>
    private lateinit var corner2ndDots: Array<RelativeLayout>
    private lateinit var pLayout: ConstraintLayout


    private var captureSessionOccupied: Int = 0 // if >10000 blocked || if <3 delayed
    private var cameraConnected: Boolean = false
    private var numBackgroundThreads: Int = 0
    private var successfulCardDetectionCounter: Int = 0
    private var lastNormalizedArea: Float = 0f
    private var toggleRepeatedCardDetection: Boolean = false

    private val repeatedCaptureRequestHandler: Handler = Handler(Looper.getMainLooper())
    private val p_w = 0.25f
    private val cardCenterC = 0.4f
    private val cardShortC = 0.15f
    private val requiredSuccessfulCardDetection: Int = 3
    private val captureRequestDelay: Long = 700 // in ms

    // some Fatal signal after onResume call.
    // restrict onResume only right after onCreate
    private var onCreateCalled: Boolean = false //

    private fun allPermissionsGranted() = REQUIRED_PERMISSION.all{
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onoCreate")
        onCreateCalled = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measure)
        // double check if permissions are granted
        if (!allPermissionsGranted()){
            Log.e(TAG, "permissions denied by the user")
            this.finish()
        }

        // init opencv
        OpenCVLoader.initDebug()

        // lateinit views
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        imageView = findViewById(R.id.thumbnail)
        cornerGuides = arrayOf(
            findViewById(R.id.cornerTL),
            findViewById(R.id.cornerTR),
            findViewById(R.id.cornerBL),
            findViewById(R.id.cornerBR)
        )
        corner1stDots = arrayOf(
            findViewById(R.id.loadingTLV1), findViewById(R.id.loadingTLH1),
            findViewById(R.id.loadingTRV1), findViewById(R.id.loadingTRH1),
            findViewById(R.id.loadingBLV1), findViewById(R.id.loadingBLH1),
            findViewById(R.id.loadingBRV1), findViewById(R.id.loadingBRH1)
        )
        corner2ndDots = arrayOf(
            findViewById(R.id.loadingTLV2), findViewById(R.id.loadingTLH2),
            findViewById(R.id.loadingTRV2), findViewById(R.id.loadingTRH2),
            findViewById(R.id.loadingBLV2), findViewById(R.id.loadingBLH2),
            findViewById(R.id.loadingBRV2), findViewById(R.id.loadingBRH2)
        )
        pLayout = findViewById(R.id.parentCTLayout)

        // deprecated. autocapture begins at onResume
        // capture btn onClickListener
        // findViewById<Button>(R.id.captureBtn).setOnClickListener {captureSurface()}

        // start cameraVideoThread
        if (numBackgroundThreads == 0) startBackgroundThread()

        // memoryspace for input image
        grImg = Mat()
        rgbImg = Mat()

        // array of point to get detected corners of card
        cornersDst = Array<Point>(4){ Point(0.0,0.0) }

        findViewById<Button>(R.id.startActivityBtn).setOnClickListener {
            Intent(this@MeasureActivity,ResultsActivity::class.java).also {
                this@MeasureActivity.startActivity(it)
            }
        }

        // This "MeasureActivity" is portrait, but it is made to look like landscape.
        textureView.setOnTouchListener(object: OnSwipeTouchListener(this@MeasureActivity){
            override fun onSwipeUp(){ // ..? still don't get it, why is this down
                Log.i(TAG, "Swipe Right")
                val set = ConstraintSet()
                set.clone(pLayout)
                set.setVerticalBias(R.id.space_center,1f)
                set.applyTo(pLayout)
                cardDetector.leftMode = false
                fingerDipDetector.leftMode = false
            }
            override fun onSwipeDown() { // ..? and why is this up
                Log.i(TAG, "Swipe Left")
                val set = ConstraintSet()
                set.clone(pLayout)
                set.setVerticalBias(R.id.space_center, 0f)
                set.applyTo(pLayout)
                cardDetector.leftMode = true
                fingerDipDetector.leftMode = true
            }
        })

        //val pLayout: ConstraintLayout = findViewById(R.id.parentCTLayout)
        val set = ConstraintSet()
        set.clone(pLayout)
        //position card corner guide spacer
        set.setDimensionRatio(R.id.space_center,"1:${cardCenterC*2}")
        // position card corner guides
        set.setVerticalBias(R.id.cornerTL,0.5f-cardShortC)
        set.setVerticalBias(R.id.cornerTR,0.5f+cardShortC)
        set.setVerticalBias(R.id.cornerBL,0.5f-cardShortC)
        set.setVerticalBias(R.id.cornerBR,0.5f+cardShortC)
        set.setHorizontalBias(R.id.cornerTL,0.5f+cardShortC*1.5857f)
        set.setHorizontalBias(R.id.cornerTR,0.5f+cardShortC*1.5857f)
        set.setHorizontalBias(R.id.cornerBL,0.5f-cardShortC*1.5857f)
        set.setHorizontalBias(R.id.cornerBR,0.5f-cardShortC*1.5857f)
        set.applyTo(pLayout)

    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause called")
        onCreateCalled = false
        if (cameraConnected) {
            cameraDevice.close()
            cameraConnected = false
        }
        stopBackgroundThread()
        toggleRepeatedCardDetection = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy called")
        onCreateCalled = false
        if (cameraConnected) {
            cameraDevice.close()
            cameraConnected = false
        }
        stopBackgroundThread()
        toggleRepeatedCardDetection = false
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        // onResume call after onPause/onDestroy/childActivity
        if (!onCreateCalled){
            this.finish()
        }
        if(numBackgroundThreads == 0) startBackgroundThread() // still not sure if i have to check the existence
        // Resume Opencv
        if(!OpenCVLoader.initDebug()){
            Log.e(TAG, "opencv init debug error")
        } else {
            baseLoaderCallback?.onManagerConnected(BaseLoaderCallback.SUCCESS)
        }
        // Resume camera2
        if(textureView.isAvailable){
            setupCamera()
            connectCamera()
            setTextureViewRatio()
            if (!this::cardDetector.isInitialized){
                Log.i(TAG, "initializing card detector")
                // initialize card detector (h,w order)
                cardDetector = CardDetector(
                    mk.ndarray(mk[previewSize.height/2,(previewSize.height*cardCenterC).toInt()]), //center left
                    mk.ndarray(mk[previewSize.height/2,previewSize.width - (previewSize.height*cardCenterC).toInt()]), //center right
                    mk.ndarray(mk[previewSize.height/2,previewSize.width - (previewSize.height*(cardShortC+cardCenterC)).toInt()]),p_w) // short p , possible window
            }
            if (!this::fingerDipDetector.isInitialized){
                Log.i(TAG, "initializing Finger dip det")
                fingerDipDetector = FingerDipDetection(
                    this,
                    Size((previewSize.height/5).toDouble(),(previewSize.height/5).toDouble()))
                fingerDipDetector.setHandResultListener()
            }
            Log.i(TAG,"onResume ${previewSize.width} ${previewSize.height}")
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }

    }

    private fun setupCamera(){
        // get ids of available camera
        val cameraIds: Array<String> = cameraManager.cameraIdList
        // choose and init a camera facing back
        for (id in cameraIds){
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK){
                continue
            }
            // i think !!. operator would be enough
            // TODO: choose between two(best res / fast)
            //previewSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
            previewSize = Size(1280,720)
            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)

            cameraId = id
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectCamera(){
        println("Is cam connected: $cameraConnected")
        if (!cameraConnected){
            cameraConnected = true
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
        }
    }

    /** Change TextureView's aspect ratio to prevent stretched image */
    private fun setTextureViewRatio(){
        val set = ConstraintSet()
        set.clone(pLayout)
        set.setDimensionRatio(textureView.id, "${previewSize.height}:${previewSize.width}")
        set.applyTo(pLayout)
    }

    /** Surface texture listener */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
            if (allPermissionsGranted()){
                setupCamera()
                connectCamera()
                setTextureViewRatio()
                if (!this@MeasureActivity::cardDetector.isInitialized){
                    // initialize card detector (h,w order)
                    cardDetector = CardDetector(
                        mk.ndarray(mk[previewSize.height/2,(previewSize.height*cardCenterC).toInt()]), //center left
                        mk.ndarray(mk[previewSize.height/2,previewSize.width - (previewSize.height*cardCenterC).toInt()]), // center right
                        mk.ndarray(mk[previewSize.height/2,previewSize.width - (previewSize.height*(cardShortC+cardCenterC)).toInt()]),p_w)
                }
                if (!this@MeasureActivity::fingerDipDetector.isInitialized){
                    fingerDipDetector = FingerDipDetection(
                        this@MeasureActivity,
                        Size((previewSize.height/5).toDouble(),(previewSize.height/5).toDouble()))
                    fingerDipDetector.setHandResultListener()
                }
                Log.i(TAG,"STListener ${previewSize.width} ${previewSize.height}")
            }
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            //TODO("Not yet implemented") <-- texture size wont change (I guess..?)
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
            //TODO("Not yet implemented") <-- all possible cases in onPause/onDestroy/onResume

            return true
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            //TODO("Not yet implemented") <-- ??
        }
    }

    /** camera State Callback */
    private val cameraStateCallback = object : CameraDevice.StateCallback(){
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            val surfaceTexture: SurfaceTexture? = textureView.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface: Surface = Surface(surfaceTexture)

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(previewSurface)

            // new api
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sessionConfig: SessionConfiguration =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(OutputConfiguration(previewSurface), OutputConfiguration(imageReader.surface)),
                        ContextCompat.getMainExecutor(this@MeasureActivity),
                        captureStateCallback
                    )
                cameraDevice.createCaptureSession(sessionConfig)
            } else { // deprecated since api level 30
                cameraDevice.createCaptureSession(
                    listOf(previewSurface, imageReader.surface),
                    captureStateCallback,
                    null)
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            //TODO("Not yet implemented")
        }

        override fun onError(camera: CameraDevice, error: Int) {
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

    /** Capture State Callback
    * for preview */
    private val captureStateCallback = object: CameraCaptureSession.StateCallback(){
        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
            try{
                cameraCaptureSession.setRepeatingRequest(
                    captureRequestBuilder.build(), null, backgroundHandler
                )
                // Resume Autocapture
                if (!toggleRepeatedCardDetection){
                    captureSurface()
                }
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(TAG, "captureStateCallback Configuration failed")
        }
    }

    /** capture callback
    * executed when capture preview requested.
    * though not sure if this is necessary  */
    private val captureCallback = object: CameraCaptureSession.CaptureCallback(){
        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
        }
    }

    /** On Image Available listener
    * This is (i think) called after successful image capture
    * any operation on captured image can be done here
    * original code converted into lambda */
    private val onImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            val image: Image = reader.acquireLatestImage()
            val buffer = image.planes[0].buffer // for JPEG, there is only one plane
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            image.close()
            /*TODO: Do whatever at here*/
            Log.i(TAG, "bitmapImage: ${bitmapImage.width} ${bitmapImage.height}")
            // the image is rotated, but rather than rotating it back, i'll use it as it is
            Utils.bitmapToMat(bitmapImage,rgbImg)
            Imgproc.cvtColor(rgbImg,grImg,Imgproc.COLOR_RGB2GRAY)

            // Just to check
            val bitmapSmall = Bitmap.createBitmap(bitmapImage.width,bitmapImage.height,Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(grImg,bitmapSmall)
            Handler(Looper.getMainLooper()).post {
                imageView.setImageBitmap(bitmapSmall)
            }

            val pointsFound = cardDetector.runDetection(grImg,cornersDst)
            Log.i(TAG, "Points found = $pointsFound")
            //Log.i(TAG, "rst0: ${cornersDst[0].x}, ${cornersDst[0].y}")
            //Log.i(TAG, "rst1: ${cornersDst[1].x}, ${cornersDst[1].y}")
//            Log.i(TAG, "rst2: ${cornersDst[2].x}, ${cornersDst[2].y}")
//            Log.i(TAG, "rst3: ${cornersDst[3].x}, ${cornersDst[3].y}")

            if (pointsFound){
                // steadiness threshold
                val newNormalizedArea = cardDetector.getDetectedAreaApprox()/cardDetector.getCardGuideArea()
                //Log.i(TAG, "last norm area : $lastNormalizedArea")
                if (abs(lastNormalizedArea - newNormalizedArea) < 0.1 ){
                    //Log.i(TAG, "new normalized area $newNormalizedArea")
                    successfulCardDetectionCounter+=1
                } else {successfulCardDetectionCounter = 0}
                lastNormalizedArea = newNormalizedArea
            } else {successfulCardDetectionCounter = 0}

            Log.i(TAG, "Successful detections $successfulCardDetectionCounter")
            // change corner guide opacity to indicate (un)successful detection
            // change additional dots to indicate detection-in-progress
            runOnUiThread {
                cornerGuides.forEach{ it.alpha = if (successfulCardDetectionCounter <= 0) 0.6f else 1f }
                corner1stDots.forEach{ it.visibility =
                    if (successfulCardDetectionCounter <= 1) View.INVISIBLE else View.VISIBLE }
                corner2ndDots.forEach{ it.visibility =
                    if (successfulCardDetectionCounter <= 2) View.INVISIBLE else View.VISIBLE }
            }



            // if had enough successful card Detection, use last picture to do hand detection
            if (successfulCardDetectionCounter >= requiredSuccessfulCardDetection) {
                val cornersDstMat = Mat(4, 2, CvType.CV_32FC1)
                for (i in 0..3) {
                    cornersDstMat.put(i, 0, cornersDst[i].x)
                    cornersDstMat.put(i, 1, cornersDst[i].y)
                }
                val M_warpToFitCard = if(cardDetector.leftMode){
                    Imgproc.getPerspectiveTransform(
                        cornersDstMat, cardDetector.offset_in_pts_l)
                } else {
                    Imgproc.getPerspectiveTransform(
                        cornersDstMat, cardDetector.offset_in_pts_r)
                }
                Imgproc.warpPerspective(
                    rgbImg,
                    rgbImg,
                    M_warpToFitCard,
                    rgbImg.size(),
                    Imgproc.INTER_LINEAR
                )
                // pixel to mm conversion coefficient
                val pixel2mm: Float = 53.98f / cardDetector.short_d
                val rgbImgBitmap =
                    Bitmap.createBitmap(rgbImg.width(), rgbImg.height(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(rgbImg, rgbImgBitmap)
                fingerDipDetector.setRGBImg(rgbImg)
                fingerDipDetector.setPixel2mm(pixel2mm)
                fingerDipDetector.runDetection(rgbImgBitmap)

                captureSessionOccupied = 5 // delay free occupation
                successfulCardDetectionCounter = 0
            } else {
                captureSessionOccupied = 0 // free occupation
            }
        }

    // Camera Video Thread Handler
    private fun startBackgroundThread(){
        numBackgroundThreads += 1
        backgroundHandlerThread = HandlerThread("CameraVideoThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(
            backgroundHandlerThread.looper
        )
    }
    private fun stopBackgroundThread(){
        numBackgroundThreads -= 1
        backgroundHandlerThread.quitSafely()
        backgroundHandlerThread.join()
    }

    /* request preview snapshot.
    * temporary implementation invoked by Capture button
    * operation on the captured image will be time consuming,
    * so a flag is used to ensure this is done one at a time.
    * Occupation should be freed at the end of operation
    * TODO: make this request automatic  */
    private fun captureSurface(){
        if (!toggleRepeatedCardDetection){
            toggleRepeatedCardDetection = true
            repeatedCaptureRequestHandler.post(object: Runnable{
                override fun run() {
                    //Log.i("RCAPHANDLER", "act: $toggleRepeatedCardDetection occ: $captureSessionOccupied")
                    if (toggleRepeatedCardDetection){
                        if(captureSessionOccupied == 0) { // no occupation
                            captureSessionOccupied = 10000 // block ( wait for 10000*captureRequestDelay ms)
                            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                            captureRequestBuilder.addTarget(imageReader.surface)
                            cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, null)
                        } else {
                            captureSessionOccupied -= 1 // wait once more
                        }
                        repeatedCaptureRequestHandler.postDelayed(this,captureRequestDelay)
                    }
                }
            })
        } else {
            toggleRepeatedCardDetection = false
        }
    }
    var mBackWait: Long = 0
    override fun onBackPressed() {
        if (System.currentTimeMillis() - mBackWait >= 2000){
            mBackWait = System.currentTimeMillis()
            Toast.makeText(
                this, "???????????? ????????? ?????? ??? ????????? ???????????????.", Toast.LENGTH_SHORT)
                .show()
        } else {
            finishAffinity()
        }
    }
}

/** Swipe gesture listener class
 * The swipe direction is based on activity's orientation.
 */
open class OnSwipeTouchListener(ctx: Context) : View.OnTouchListener {

    private val gestureDetector: GestureDetector

    companion object {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100
    }

    init {
        gestureDetector = GestureDetector(ctx, GestureListener())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var result = false
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if ((Math.abs(diffX) + Math.abs(diffY) > 2*SWIPE_THRESHOLD) &&
                    (Math.abs(velocityX) + Math.abs(velocityY) > 2* SWIPE_VELOCITY_THRESHOLD)){

                    if (diffY >= diffX && diffY > -diffX){
                        onSwipeUp()
                    } else if (diffY < diffX && diffY >= -diffX ){
                        onSwipeRight()
                    } else if (diffY < diffX && diffY < -diffX){
                        onSwipeDown()
                    } else {
                        onSwipeLeft()
                    }
                    result = true
                } else {
                    result = false
                }
            } catch (exception: Exception){
                exception.printStackTrace()
            }
            return result
        }
    }
    open fun onSwipeDown() {}

    open fun onSwipeLeft() {}

    open fun onSwipeRight() {}

    open fun onSwipeUp() {}
}