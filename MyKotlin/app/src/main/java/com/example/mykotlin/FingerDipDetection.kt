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
import org.opencv.core.*

class FingerDipDetection(
    context:Context,
    _cropSize: Size,
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

    private val cropSize: Size
    private val wand: MagicWand
    val boolImg16S: Mat
    private var crop_dx: Mat
    private var xv: Mat
    private var xvdx: Mat

    val thicknessList: FloatArray


    init {
        hands.setErrorListener { message, e -> Log.e("fingerDipdet", message)}

        cropSize = _cropSize
        wand = MagicWand(_cropSize)
        boolImg16S = Mat(_cropSize,CvType.CV_16SC1)
        crop_dx = Mat.zeros(_cropSize,CvType.CV_16SC1)
        xv = Mat(_cropSize,CvType.CV_16SC1)
        val x = ShortArray((_cropSize.width*_cropSize.height).toInt()){
                i->(i%_cropSize.width.toInt()).toShort()}
        xv.put(0,0,x)
        xvdx = Mat(_cropSize,CvType.CV_16SC1)

        thicknessList = FloatArray(100){0f}
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
        assert(crop.size() == cropSize)
        val c_h: Int = cropSize.height.toInt()
        val c_w: Int = cropSize.width.toInt()
        wand.applyWand(crop, intArrayOf(c_h/2,c_w/2),60,boolImg16S)
        Core.subtract(
            boolImg16S.submat(0,c_h,1,c_w),
            boolImg16S.submat(0,c_h,0,c_w-1),
            crop_dx.submat(0,c_h,1,c_w)
        )
        // simple check if using submat as dst works
        val mml = Core.minMaxLoc(crop_dx)
        println("crop_dx min ${mml.minVal}, max ${mml.maxVal}")

        Core.multiply(xv,crop_dx,xvdx)
        val leftEdgeX = Array<Int>(c_h){
            val maxLocResult = Core.minMaxLoc(
                xvdx.submat(it,it+1,0,(c_w*0.7).toInt()))
            maxLocResult.maxLoc.x.toInt()
        }
        Core.multiply(crop_dx, Scalar(-c_w.toDouble()),crop_dx)
        Core.add(xvdx,crop_dx,xvdx)
        val rightEdgeX = Array<Int>(c_h){
            val maxLocResult = Core.minMaxLoc(
                xvdx.submat(it,it+1,(c_w*0.3).toInt(),c_w))
            maxLocResult.maxLoc.x.toInt() + (c_w*0.3).toInt()
        }

        val d_c_h = Array<Int>(c_h){0} // distances(thickness) array of size c_h(before norm)
        val local_d = Array<Int>(2){0}
        // algorithm that goes back and forth between left and right edge
        var p0_y = 0
        var p1_y = 0
        while (true){
            var p0_x = leftEdgeX[p0_y]

        }
    }

}