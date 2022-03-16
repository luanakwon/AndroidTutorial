package com.example.mykotlin

import android.content.IntentFilter
import android.util.Log
import com.google.android.material.resources.MaterialAttributes
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.linalg.inv
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class CardDetector(center: D1Array<Int>, short_p: D1Array<Int>, val p_w: Float) {
    // center and short_p in (y,x) or (h,w) order
    val input_pts: D2Array<Float>
    val area: Float
    val offset_in_pts: Mat
    val offset_out_pts: Mat
    val short_d: Int
    val long_d: Int
    val M_in2out: Mat
    val M_out2in: Mat
    // reusable Mat for performance
    var ld : Int
    var ld2 : Int
    var sd : Int
    var sd2 : Int
    //var pedge_l: Mat
    var pedge_s: Mat
    //var mat_d_l: Mat
    var mat_d_s: Mat
    //var scores_l: Mat
    var scores_s: Mat
    var dummy: Mat
    var peTBLR: Array<Mat>
    var peLR: Mat
    var dst: Mat
    var corners: Mat
    var corners_col2: Mat
    var linRegThresConst: Float
    //var xv_l: Mat
    var xv_s: Mat
    //var yv_l: Mat
    var yv_s: Mat
    //var lb_l: Mat
    var lb_s: Mat
    var rb_lb_s: Mat
    //var rb_lb_l: Mat
    //var ones_hw_l: Mat
    val ones_hw_s: Mat
    //var ones_hh_l: Mat
    val ones_hh_s: Mat

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

        // reusable Mat for performance
        ld = (long_d*(p_w/(p_w+1))).toInt()
        ld2 = long_d-ld
        sd = (short_d*(p_w/(p_w+1))).toInt()
        sd2 = short_d - sd

        // long: width ld2-ld, height sd
        // pedge_l = Mat(sd,ld2-ld,CvType.CV_32FC1)
        // short: width sd2-sd, height ld
        pedge_s = Mat(ld,(sd2-sd)/4,CvType.CV_32FC1)

        // mat_d_l = Mat((ld2-ld)*sd,sd*sd,CvType.CV_32FC1)
        mat_d_s = Mat((sd2-sd)*ld,ld*ld,CvType.CV_32FC1)
        // scores_l = Mat(1,sd*sd,CvType.CV_32FC1)
        scores_s = Mat(1,ld*ld,CvType.CV_32FC1)
        dummy = Mat() // I'm not sure if size doesn't matter or if it's reallocated every time
        peLR = Mat(ld,sd2-sd,CvType.CV_8UC1)
        peTBLR = Array(4){ Mat(pedge_s.size(), CvType.CV_8UC1)}
        dst = Mat(short_d,long_d,CvType.CV_8UC1)
        corners = Mat(3,4,M_out2in.type())
        corners_col2 = Mat(3,4,corners.type())

        linRegThresConst = 0.8f
        //xv_l = Mat(pedge_l.width()*pedge_l.height(),1,CvType.CV_32FC1)
        //yv_l = Mat(pedge_l.width()*pedge_l.height(),1,CvType.CV_32FC1)
        xv_s = Mat(pedge_s.width()*pedge_s.height(),1,CvType.CV_32FC1)
        yv_s = Mat(pedge_s.width()*pedge_s.height(),1,CvType.CV_32FC1)

        //var x = FloatArray(pedge_l.width()*pedge_l.height()){i->(i%pedge_l.width()).toFloat()}
        //xv_l.put(0,0,x)
        //x = FloatArray(pedge_l.width()*pedge_l.height()){i->(i/pedge_l.width()).toFloat()}
        //yv_l.put(0,0,x)
        var x = FloatArray(pedge_s.width()*pedge_s.height()){i->(i%pedge_s.width()).toFloat()}
        xv_s.put(0,0,x)
        x = FloatArray(pedge_s.width()*pedge_s.height()){i->(i/pedge_s.width()).toFloat()}
        yv_s.put(0,0,x)

        //lb_l = Mat(1,pedge_l.height()*pedge_l.height(),CvType.CV_32FC1)
        //rb_lb_l = Mat(1,pedge_l.height()*pedge_l.height(),CvType.CV_32FC1)
        lb_s = Mat(1,pedge_s.height()*pedge_s.height(),CvType.CV_32FC1)
        rb_lb_s = Mat(1,pedge_s.height()*pedge_s.height(),CvType.CV_32FC1)

        //x = FloatArray(pedge_l.height()*pedge_l.height()){i->(i%pedge_l.height()).toFloat()}
        //lb_l.put(0,0,x)
        //x = FloatArray(pedge_l.height()*pedge_l.height()){i->(i/pedge_l.height()).toFloat()}
        //rb_lb_l.put(0,0,x)
        x = FloatArray(pedge_s.height()*pedge_s.height()){i->(i%pedge_s.height()).toFloat()}
        lb_s.put(0,0,x)
        x = FloatArray(pedge_s.height()*pedge_s.height()){i->(i/pedge_s.height()).toFloat()}
        rb_lb_s.put(0,0,x)


        //Core.subtract(rb_lb_l,lb_l,rb_lb_l)
        Core.subtract(rb_lb_s,lb_s,rb_lb_s)

        //ones_hw_l = Mat.ones(pedge_l.height()*pedge_l.width(),1,CvType.CV_32FC1)
        ones_hw_s = Mat.ones(pedge_s.height()*pedge_s.width(),1,CvType.CV_32FC1)

        //ones_hh_l = Mat.ones(1, pedge_l.height()*pedge_l.height(),CvType.CV_32FC1)
        ones_hh_s = Mat.ones(1, pedge_s.height()*pedge_s.height(),CvType.CV_32FC1)
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
    private fun linear_regression2(/*weighted_img = pedge  isLong: Boolean*/): Pair<Int,Int>{ //TODO: Out of Mem
        Log.i("CDD","linreg called")
        val weighted_img = pedge_s
        val mat_d = mat_d_s
        val scores = scores_s
        val xv = xv_s
        val yv = yv_s
        val lb = lb_s
        val rb_lb = rb_lb_s
        val ones_hw = ones_hw_s
        val ones_hh = ones_hh_s

        val height = weighted_img.size().height.toInt()
        val width = weighted_img.size().width.toInt()
        Log.i("CDD", "before meshgrid")

        Log.i("CDD","after meshgrid")

        Log.i("CDD", "bf linalg dot lb")

        Log.i("CDD", "bf linalg dot rb")

        Log.i("CDD", "bf linalg dot mkd")
//        val mk_d = (mk.linalg.dot(xv.reshape(width*height,1),(rb-lb))/(width.toFloat())
//        + mk.linalg.dot(mk.ones(height*width, 1),lb)
//        - mk.linalg.dot(yv.reshape(height*width,1),mk.ones(1,height*height)))

        Core.gemm(xv,rb_lb,width.toDouble(),dummy,0.0,mat_d)
        Core.gemm(ones_hw,lb,1.0,mat_d,1.0,mat_d)
        Core.gemm(yv,ones_hh,-1.0,mat_d,1.0,mat_d)

        // val mat_d = Mat(width*height,height*height,CvType.CV_32FC1)
//        mat_d.put(0,0,mk_d.toFloatArray())

        Core.pow(mat_d,2.0,mat_d)
        Core.add(mat_d, Scalar(1.0),mat_d)
        Core.divide(1.0,mat_d,mat_d)

        //val scores = Mat()
        Core.gemm(
            weighted_img.reshape(0,1),
            mat_d,1.0,
            dummy,0.0,
            scores)

        val mmlr = Core.minMaxLoc(scores)
        return if(mmlr.maxVal < 0.8*width*linRegThresConst){
            Pair(0,0)
        } else {
            Pair(mmlr.maxLoc.x.toInt()%height,mmlr.maxLoc.x.toInt()/height)
        }

    }
    private fun getPossibleEdge(/*dst:Mat*/): Unit{

        //possible_edges.add(dst.submat(0,sd,ld,ld2))
        Imgproc.resize(dst.submat(0,sd,ld,ld2),peTBLR[0],peTBLR[0].size())
        Imgproc.resize(dst.submat(sd2,short_d,ld,ld2),peTBLR[1],peTBLR[1].size())
        //possible_edges.add(dst.submat(sd2,short_d,ld,ld2))
        Core.rotate(dst.submat(sd,sd2,0,ld)
            ,peLR,Core.ROTATE_90_CLOCKWISE)
        Imgproc.resize(dst.submat(0,sd,ld,ld2),peTBLR[2],peTBLR[2].size())
        //possible_edges.add(peLeft)
        Core.rotate(dst.submat(sd,sd2,ld2,long_d)
            ,peLR,Core.ROTATE_90_CLOCKWISE)
        Imgproc.resize(dst.submat(0,sd,ld,ld2),peTBLR[3],peTBLR[3].size())
        //possible_edges.add(peRight)
    }

    fun runDetection(grimg: Mat, corners_dst: Array<Point>): Boolean {
        // orientation: landscape

        Imgproc.warpPerspective(
            grimg, dst, M_in2out, Size(long_d.toDouble(),short_d.toDouble()), Imgproc.INTER_LINEAR
        )

        getPossibleEdge(/*dst*/)
        // val sup_v = pe_pair.second // ld ld2 sd sd2

        val pt8 = mk.zeros<Int>(4,4)
        peTBLR.forEachIndexed{i, _pedge ->
            // first two is long edge, second two is short edge
            //val pedge = if (i < 2) pedge_l else pedge_s
            val pedge = pedge_s
            Core.normalize(_pedge,pedge,0.0,1.0,Core.NORM_MINMAX,CvType.CV_32F)

            Imgproc.Scharr(pedge,pedge,-1,0,1)
            Core.pow(pedge,2.0,pedge)

            val lb_rb = linear_regression2()
            if(lb_rb.first != 0 || lb_rb.second != 0){
                if(i==0){
                    pt8[i,0] = ld
                    pt8[i,1] = lb_rb.first*sd/ld
                    pt8[i,2] = ld2-1
                    pt8[i,3] = lb_rb.second*sd/ld
                } else if(i==1){
                    pt8[i,0] = ld
                    pt8[i,1] = sd2+(lb_rb.first*sd/ld)
                    pt8[i,2] = ld2-1
                    pt8[i,3] = sd2+(lb_rb.second*sd/ld)
                } else if(i==2){
                    pt8[i,0] = lb_rb.first
                    pt8[i,1] = sd2-1
                    pt8[i,2] = lb_rb.second
                    pt8[i,3] = sd
                } else{
                    pt8[i,0] = ld2+lb_rb.first
                    pt8[i,1] = sd2-1
                    pt8[i,2] = ld2+lb_rb.second
                    pt8[i,3] = sd
                }
            } else {
                pt8[i,0] = -1
                pt8[i,1] = -1
                pt8[i,2] = -1
                pt8[i,3] = -1
            }
        }

        val A = mk.zeros<Double>(8,2)
        val C = mk.zeros<Double>(8,1)
        corners.setTo(Scalar(1.0))
        var points_found = true
        pt8.forEach {
            if (it < 0) {
                points_found = false
            }
        }
        if(points_found){
            for (i in 0..3){
                val p = pt8[i]
                C[i*2,0] = (p[0]*p[3] - p[2]*p[1]).toDouble()
                A[i*2,0] = (p[3]-p[1]).toDouble()
                A[i*2,1] = (p[0]-p[2]).toDouble()
            }
            A[1] = A[4].copy()
            A[3] = A[6].copy()
            A[5] = A[2].copy()
            A[7] = A[0].copy()

            C[1] = C[4].copy()
            C[3] = C[6].copy()
            C[5] = C[2].copy()
            C[7] = C[0].copy()

            intArrayOf(0,6,2,4).forEachIndexed{idx,i->
                val resultB = mk.linalg.dot(
                    mk.linalg.inv(A[IntRange(i,i+2)]),C[IntRange(i,i+2)])
                    .flatten()
                corners.put(0,idx,resultB[0])
                corners.put(1,idx,resultB[1])
            }
            Core.gemm(M_out2in,corners,1.0,dummy,0.0,corners)
            //val corners_col2 = Mat()
            Core.repeat(corners.submat(2,3,0,4),3,1,corners_col2)
            Core.divide(corners,corners_col2,corners)

            for (i in 0..3){
                corners_dst[i].x = corners[0,i][0]
                corners_dst[i].y = corners[1,i][0]
            }
        }

        return points_found
    }
}