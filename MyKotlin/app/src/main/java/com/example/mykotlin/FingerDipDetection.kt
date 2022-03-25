package com.example.mykotlin

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.solutioncore.ResultListener
import com.google.mediapipe.solutions.hands.HandLandmark
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat

class FingerDipDetection(
    context:Context,
    static_image_mode:Boolean = true,
    max_num_hands:Int = 1,
    model_complexity:Int = 0,
    min_detection_confidence:Float = 0.5f
){

    private val handsOptions = HandsOptions.builder()
        .setStaticImageMode(static_image_mode)
        .setMaxNumHands(max_num_hands)
        .setRunOnGpu(true)
        .setModelComplexity(model_complexity)
        .setMinDetectionConfidence(min_detection_confidence)
        .build()
    private val hands = Hands(context,handsOptions)
    private val fingerDipIndices = intArrayOf(
        HandLandmark.INDEX_FINGER_DIP,
        HandLandmark.MIDDLE_FINGER_DIP,
        HandLandmark.MIDDLE_FINGER_DIP,
        HandLandmark.PINKY_DIP
    )
    private var crop_dx: Mat
    private var xv: Mat


    val thicknessList = FloatArray(100){0f}


    init {
        hands.setErrorListener { message, e -> Log.e("fingerDipdet", message)}

        //lateinit variables
        crop_dx = Mat(128,128,CvType.CV_16SC1)
        xv = Mat(128,128,CvType.CV_16SC1)
    }

    val resultListener = object : ResultListener<HandsResult>{
        override fun run(result: HandsResult?) {
            // error: no result
            if (result == null) {return}
            // no hand found
            if (result.multiHandLandmarks().isEmpty()) {return}
            // hand(s) found
            val width = result.inputBitmap().width
            val height = result.inputBitmap().height
            val handLandmarks = result.multiHandLandmarks()[0].landmarkList
            val fingerDips = IntArray(4*2){0} // four dips, xy each
            val fingerDirs = FloatArray(4*2){0f} // same
            val ret:Boolean = false

            fingerDipIndices.forEachIndexed { index, i ->
                fingerDips[2*index+0] = (handLandmarks[i].x*width).toInt()
                fingerDips[2*index+1] = (handLandmarks[i].y*height).toInt()
                fingerDirs[2*index+0] = (handLandmarks[i+1].x - handLandmarks[i].x)*width
                fingerDirs[2*index+1] = (handLandmarks[i+1].y - handLandmarks[i].y)*height
            }
        }
    }
    fun runDetection(img:Bitmap){ // assuming bitmap is not null
        hands.send(img)
    }
    fun setHandResultListener(){
        hands.setResultListener(resultListener)
    }
    // returns height-wise normalized 100 thickness pixel values
    fun shoelace(crop:Mat): Unit{
        val c_h = crop.height()
        val c_w = crop.width()

    }

}