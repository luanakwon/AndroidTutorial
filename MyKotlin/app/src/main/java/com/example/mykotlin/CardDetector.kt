package com.example.mykotlin

import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

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
    fun linear_regression2(weighted_img:Mat): Pair<Int,Int>{
        val height = weighted_img.size().height.toInt()
        val width = weighted_img.size().width.toInt()

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

        val mk_sq_d = 1f / mk.linalg.pow(
                mk.linalg.dot(xv.reshape(width*height,1),(rb-lb))/(width.toFloat())
                + mk.linalg.dot(mk.ones(height*width, 1),lb)
                - mk.linalg.dot(
                    yv.reshape(height*width,1),mk.ones(1,height*height))
                ,2)
        val sq_d = Mat(width*height,height*height,weighted_img.type())
        sq_d.put(0,0,mk_sq_d.toFloatArray())

        val scores = Mat()
        Core.gemm(weighted_img.reshape(1,height*width),sq_d,0.0,null,0.0,scores)

        val mmlr = Core.minMaxLoc(scores)
        return if(mmlr.maxVal < 0.8*width*0.3){
            Pair(0,0)
        } else {
            Pair(lb[0,mmlr.maxLoc.x.toInt()].toInt(),rb[0,mmlr.maxLoc.x.toInt()].toInt())
        }
//        val scores = mk.linalg.dot(weighted_img.reshape(1,height*width), sq_d)
//        val idx = mk.math.argMax(scores)
//        return if(scores[0,idx] < 0.8*width*0.3){
//            intArrayOf(0,0)
//        } else {
//            intArrayOf(lb[0,idx].toInt(),rb[0,idx].toInt())
//        }
    }
    fun getPossibleEdge(dst:Mat): Pair<ArrayList<Mat>,IntArray>{
        val ld = (long_d*(p_w/(p_w+1))).toInt()
        val ld2 = long_d-ld
        val sd = (short_d*(p_w/(p_w+1))).toInt()
        val sd2 = short_d - sd

        val possible_edges = arrayListOf<Mat>()
        possible_edges.add(dst.submat(0,sd,ld,ld2))
        possible_edges.add(dst.submat(sd2,short_d,ld,ld2))
        val peLeft = Mat()
        Core.rotate(dst.submat(sd,sd2,0,ld)
            ,peLeft,Core.ROTATE_90_CLOCKWISE)
        possible_edges.add(peLeft)
        val peRight = Mat()
        Core.rotate(dst.submat(sd,sd2,ld2,long_d)
            ,peRight,Core.ROTATE_90_CLOCKWISE)
        possible_edges.add(peRight)

        return Pair(possible_edges, intArrayOf(ld,ld2,sd,sd2))
    }
    fun run(grimg: Mat){
        // orientation: landscape
        val dst = Mat()
        Imgproc.warpPerspective(
            grimg, dst, M_in2out, Size(long_d.toDouble(),short_d.toDouble()), Imgproc.INTER_LINEAR
        )
        println("145 231")
        val pe_pair = getPossibleEdge(dst)
        val possible_edges = pe_pair.first
        val sup_v = pe_pair.second

        val pt8 = mk.zeros<Int>(4,4)
        possible_edges.forEachIndexed{i, _pedge ->
            val pedge = Mat()
            Core.normalize(_pedge,pedge,0.0,1.0,Core.NORM_MINMAX,CvType.CV_32F)

            var dysq = Mat()
            Imgproc.Scharr(pedge,dysq,-1,0,1)
            Core.pow(dysq,2.0,dysq)

            val lb_rb = linear_regression2(dysq)
            if(lb_rb.first != 0 || lb_rb.second != 0){

            } else {

            }

        }
    }
}