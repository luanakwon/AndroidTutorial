package com.onbleep.ringgauge

import android.util.Log
import org.opencv.core.*

class MagicWand(s:Size) {
    private val width:Int
    private val height:Int
    private var intImg:Mat
    private var channels: MutableList<Mat>
    private var maxImg: Mat
    private var boolImg8U: Mat

    init {
        width = s.width.toInt()
        height = s.height.toInt()

        intImg = Mat(s,CvType.CV_16SC3)
        channels = Array<Mat>(3){
            Mat(s,CvType.CV_16SC1)
        }.toMutableList()
        maxImg = Mat(s,CvType.CV_16SC1)
        boolImg8U = Mat(s,CvType.CV_8UC1)
    }

    /** dst size == img size, dst type 16SC1 */
    fun applyWand(img: Mat, point:IntArray, tol: Int, dst:Mat){
        val c0: DoubleArray = img[point[0],point[1]]
        img.convertTo(intImg,CvType.CV_16SC3)
        Core.subtract(intImg,Scalar(c0),intImg)

        val channels = ArrayList<Mat>()
        Core.split(intImg,channels)

        println(channels.size)
        // assert 3 channel
        if (channels.size < 3){
            Log.e("MagicWand","channel size ${channels.size} < 3")
            return
        }
        // put max of channels at maxImg
        Core.max(channels[0],channels[1], maxImg)
        Core.max(channels[2],maxImg, maxImg)
        // put min of channels at channels[0]
        Core.min(channels[0],channels[1],channels[0])
        Core.min(channels[0],channels[2], channels[0])
        // maxImg - minImg
        Core.subtract(maxImg,channels[0],maxImg)
        val mml = Core.minMaxLoc(maxImg)
        Log.i("MagicWand", "maxImg-minImg min: ${mml.minVal}, max: ${mml.maxVal}, tol: $tol")
        Core.compare(maxImg, Scalar(tol.toDouble()), boolImg8U, Core.CMP_LT) // true=255 false=0
        Core.divide(boolImg8U, Scalar(255.0),dst,1.0,CvType.CV_16SC1)
    }

}