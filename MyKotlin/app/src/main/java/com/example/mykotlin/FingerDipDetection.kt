package com.example.mykotlin

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.solutioncore.ResultListener
import com.google.mediapipe.solutions.hands.HandLandmark
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import org.opencv.android.Utils
import org.opencv.core.*
import kotlin.math.sqrt

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

    private val hostContext: Context
    private val cropSize: Size
    private val wand: MagicWand
    private val boolImg16S: Mat
    private var crop_dx: Mat
    private var xv: Mat
    private var xvdx: Mat
    private var rgbImg: Mat
    private var pixel2mmCoef: Float

    val thicknessList: FloatArray


    init {
        hostContext = context
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
        rgbImg = Mat()
        pixel2mmCoef = 0f
    }

    val resultListener = object : ResultListener<HandsResult>{
        override fun run(result: HandsResult?) {
            // error: no result
            if (result == null) {
                Log.e("FDD", "no result")
                return
            }
            // no hand found
            if (result.multiHandLandmarks().isEmpty()) {
                Log.i("FDD", "no hands found")
                return
            }
            // too many hands found
            if (result.multiHandLandmarks().size > 1){
                Log.i("FDD", "too many hands found")
                return
            }
            // hand(s) found
            val width = result.inputBitmap().width
            val height = result.inputBitmap().height
            val handLandmarks = result.multiHandLandmarks()[0].landmarkList
            val fingerDips = IntArray(4*2){0} // four dips, xy each
            val fingerDirs = FloatArray(4*2){0f} // same
            // val ret:Boolean = false

            val estimated4Thicknesses = Array(4){0f}
            fingerDipIndices.forEachIndexed { index, i ->
                fingerDips[2*index+0] = (handLandmarks[i].x*width).toInt()
                fingerDips[2*index+1] = (handLandmarks[i].y*height).toInt()
                fingerDirs[2*index+0] = (handLandmarks[i+1].x - handLandmarks[i].x)*width
                fingerDirs[2*index+1] = (handLandmarks[i+1].y - handLandmarks[i].y)*height

                // find crop roi (center = fingerDipCor, size = CropSize)
                val cropCenterX = fingerDips[2*index+0]
                val cropCenterY = fingerDips[2*index+1]
                val dirW = fingerDirs[2*index+0]
                val dirH = fingerDirs[2*index+1]
                // assuming crop shape is square
                val dRadius = (cropSize.width/2).toInt()
                val croppedSubmat = rgbImg.submat(
                    cropCenterY - dRadius, cropCenterY + dRadius,
                    cropCenterX - dRadius, cropCenterX + dRadius)
                val dMagnitude = sqrt(dirW*dirW + dirH*dirH)
                val dstX = Array(100){0f}
                val dstY = Array(100){0f}
                // wand and shoelace
                shoelace(croppedSubmat,dMagnitude,dstX,dstY)
                // TODO: add correction layer that returns thickness in pixel
                // temporarily implemented as mean
                estimated4Thicknesses[i] = pixel2mmCoef*dstY.sum()/dstY.size
                println("[$i] x: $cropCenterX y: $cropCenterY thickness: ${estimated4Thicknesses[i]}")
            }

            // TODO: start next activity with
            // estimated4Thicknesses, rgbImgBitmap
            Toast.makeText(hostContext
                ,"${estimated4Thicknesses[0]} ${estimated4Thicknesses[1]} ${estimated4Thicknesses[2]} ${estimated4Thicknesses[3]} "
                ,Toast.LENGTH_SHORT).show()
        }
    }
    fun runDetection(img:Bitmap){ // assuming bitmap is not null
        hands.send(img)
    }
    fun setHandResultListener(){
        hands.setResultListener(resultListener)
    }
    fun setRGBImg(img: Mat){
        rgbImg = img
    }
    fun setPixel2mm(coef: Float){
        pixel2mmCoef = coef
    }
    // returns height-wise normalized 100 thickness pixel values
    fun shoelace(crop:Mat, dMagnitude: Float, dstX:Array<Float>, dstY:Array<Float>): Unit{
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
        var leftEdgeX = Array<Int>(c_h){
            val maxLocResult = Core.minMaxLoc(
                xvdx.submat(it,it+1,0,(c_w*0.7).toInt()))
            maxLocResult.maxLoc.x.toInt()
        }
        Core.multiply(crop_dx, Scalar(-c_w.toDouble()),crop_dx)
        Core.add(xvdx,crop_dx,xvdx)
        var rightEdgeX = Array<Int>(c_h){
            val maxLocResult = Core.minMaxLoc(
                xvdx.submat(it,it+1,(c_w*0.3).toInt(),c_w))
            maxLocResult.maxLoc.x.toInt() + (c_w*0.3).toInt()
        }

        val d_c_h = Array<Float>(c_h){0f} // distances(thickness) array of size c_h(before norm)
        val local_d = Array<Float>(2){0f}
        // algorithm that goes back and forth between left and right edge
        var p0_y = 0
        var p1_y = 0
        while (true){
            val p0_x = leftEdgeX[p0_y]
            var isInEdge = false
            for (i in local_d.indices){
                if (p1_y+i < rightEdgeX.size){
                    isInEdge = true
                    local_d[i] = (sqrt(
                        (p1_y+i-p0_y)*(p1_y+i-p0_y).toFloat()
                        +(p0_x-rightEdgeX[p1_y+i])*(p0_x-rightEdgeX[p1_y+i]).toFloat()))
                }
            }
            if (!isInEdge){
                break
            }
            // find min and argmin of local_d
            var minVal = local_d[0]
            var minLoc = 0
            for (i in 1 until local_d.size){
                if (minVal > local_d[i]){
                    minVal = local_d[i]
                    minLoc = i
                }
            }
            d_c_h[p0_y] = minVal
            // Flip edge. (name 'left' and 'right' doesn't mean anything from now on)
            val tList = leftEdgeX
            leftEdgeX = rightEdgeX
            rightEdgeX = tList
            // Flip y pointer(indexer)
            val tp = p0_y+1
            p0_y = p1_y+minLoc
            p1_y = tp
        }
        // TODO: smooth the outliers such as thickness == 0

        // y and thickness normed y-wise by dir magnitude
        for (i in dstX.indices){
            dstX[i] = -c_h*dstX.size/(2*dMagnitude)+i*c_h/dMagnitude
        }
        for (i in dstY.indices){
            val x: Float = i*c_h.toFloat()/100.0f
            if (0 <= x && (x.toInt()+1) < c_h){
                val y0:Float = d_c_h[x.toInt()]
                val y1:Float = d_c_h[x.toInt()+1]
                val r :Float = x-x.toInt()
                dstY[i] = (1-r)*y0 + r*y1
            } else if ((x.toInt()+1) == c_h){
                dstY[i] = d_c_h[c_h-1]
            }
        }
    }
}