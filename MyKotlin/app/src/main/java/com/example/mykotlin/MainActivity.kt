package com.example.mykotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import org.jetbrains.kotlinx.multik.api.arange
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.ones
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OpenCVLoader.initDebug()


        val CDD = CardDetector(mk.ndarray(mk[170,140]),mk.ndarray(mk[160,200]),0.2f)
        var textView = findViewById<TextView>(R.id.textView)
        textView.text = CDD.area.toString()

        for (i in 0..2){
            for (j in 0..2){
                println("$i    $j   ${CDD.M_in2out.get(i,j).get(0)}")
            }
        }
        println()

        val grimg = Mat.ones(360,640,CvType.CV_8U)
        println(grimg)
        CDD.run(grimg)



    }

}