package com.onbleep.ringgauge

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import org.opencv.android.Utils
import org.opencv.core.Mat

class ResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        val thicknesses = intent.getFloatArrayExtra("EXTRA_ESTIMATED4THICKNESSES")
        val matAddr = intent.getLongExtra("EXTRA_RGBMATADDR",0L)

        println("MAIN2")
        if (thicknesses != null){
            println("index:  ${thicknesses[0]}")
            println("middle: ${thicknesses[1]}")
            println("ring:   ${thicknesses[2]}")
            println("pinky:  ${thicknesses[3]}")
        } else {println("null thickness")}
        if (matAddr != 0L){
            println("img mat addr: $matAddr")
            val matImg = Mat(matAddr)
            val bitmapImg = Bitmap.createBitmap(matImg.size().width.toInt(),matImg.size().height.toInt(),Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(matImg,bitmapImg)
            findViewById<ImageView>(R.id.pictureResult).setImageBitmap(bitmapImg)
        } else {println("no image addr")}
    }
}