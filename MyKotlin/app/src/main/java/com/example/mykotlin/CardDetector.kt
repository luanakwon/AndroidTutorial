package com.example.mykotlin

import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import kotlin.reflect.typeOf

class CardDetector(center: D1Array<Int>, short_p: D1Array<Int>, val p_w: Float) {

    val input_pts: D2Array<Float>
    val area: Float
    val offset_in_pts: Mat
    val offset_out_pts: Mat
    val short_d: Int
    val long_d: Int
    val M_in2out: Mat
    val M_out2in: Mat

    init {
        val short_v = ((short_p-center)*2).asType<Float>()
        val rtarray = mk.ndarray(floatArrayOf(0f, -1.5857f, 1.5857f, 0f),2,2)
        val long_v = mk.linalg.dot(short_v.reshape(1,2),rtarray).reshape(2)
        val start_p = short_p.asType<Float>() - (long_v / 2f)

        input_pts = mk.stack(mk[
                start_p,start_p+long_v,start_p-short_v+long_v,start_p-short_v])
        area = (mk.linalg.norm(short_v.reshape(1,2))*
                mk.linalg.norm(long_v.reshape(1,2))).toFloat()

        short_v *= (1+p_w)
        long_v *= (1+p_w)
        val start_p2 = center.asType<Float>() + short_v/2f - long_v/2f

        offset_in_pts = Mat(4,2,CvType.CV_32F)
        offset_in_pts.put(0,0,
            mk.linalg.dot(
                mk.stack(mk[start_p2,start_p2+long_v,start_p2-short_v+long_v,start_p2-short_v]),
                mk.ndarray(mk[mk[0f,1f],mk[1f,0f]])).toFloatArray())

        short_d = mk.linalg.norm(short_v.reshape(1,2)).toInt()
        long_d = mk.linalg.norm(long_v.reshape(1,2)).toInt()

        offset_out_pts = Mat(4,2,CvType.CV_32F)
        offset_out_pts.put(0,0,
            mk.ndarray(mk[mk[0,0],mk[long_d,0],mk[long_d,short_d],mk[0,short_d]])
                .asType<Float>().toFloatArray())

        M_in2out = Imgproc.getPerspectiveTransform(
            offset_in_pts,offset_out_pts)
        M_out2in = Imgproc.getPerspectiveTransform(
            offset_out_pts,offset_in_pts)

    }

    fun getCardGuidePoint(order:String = "xy"): D2Array<Float>{
        if (order == "xy"){
            return mk.linalg.dot(input_pts,mk.ndarray(mk[mk[0f,1f],mk[1f,0f]]))
        }
        // else
        return input_pts
    }
    fun getCardGuideArea(): Float{
        return area
    }
    fun linear_regression2(weighted_img:D2Array<Float>, threshold:Float): IntArray{
        val height = weighted_img.shape[0]
        val width = weighted_img.shape[1]

        val x = mk.arange<Float>(width)
        val y = mk.arange<Float>(height)
        val xv_yv = mk.meshgrid(x,y)

        val xv = xv_yv.first
        val yv = xv_yv.second

        val lb = mk.linalg.dot(
            mk.ones(height,1),
            mk.arange<Float>(height).reshape(1,height)
        ).reshape(1,height*height)
        val rb = mk.linalg.dot(
            mk.arange<Float>(height).reshape(height,1),
            mk.ones(1,height)
        ).reshape(1,height*height)

        val sq_d = 1f / mk.linalg.pow(
                mk.linalg.dot(xv.reshape(width*height,1),(rb-lb))/(width.toFloat())
                + mk.linalg.dot(mk.ones(height*width, 1),lb)
                - mk.linalg.dot(
                    yv.reshape(height*width,1),mk.ones(1,height*height))
                ,2)

        val scores = mk.linalg.dot(weighted_img.reshape(1,height*width), sq_d)
        val idx = mk.math.argMax(scores)
        return if(scores[0,idx] < 0.8*width*0.3){
            intArrayOf(0,0)
        } else {
            intArrayOf(lb[0,idx].toInt(),rb[0,idx].toInt())
        }
    }
}