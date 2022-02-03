package com.example.mykotlin

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class MagicWand {
    companion object{
        fun apply(img: Mat, point:IntArray, tol: Int): Mat{
            val c0 = img[point[0],point[1]][0]
            val intimg = Mat()
            img.convertTo(intimg,CvType.CV_32S)
            Core.subtract(intimg,Scalar(c0),intimg)

            val maxImg = Mat()
            val minImg = Mat()
            val channels = ArrayList<Mat>()
            Core.split(intimg,channels)
            Core.max(channels[0],channels[1], maxImg)
            Core.max(channels[2],maxImg, maxImg)
            Core.min(channels[0],channels[1],minImg)
            Core.min(channels[2],minImg, minImg)

            val boolImg = Mat()
            Core.subtract(maxImg,minImg,boolImg)
            Core.compare(boolImg, Scalar(tol.toDouble()), boolImg, Core.CMP_LT)

            return boolImg
        }
    }
}