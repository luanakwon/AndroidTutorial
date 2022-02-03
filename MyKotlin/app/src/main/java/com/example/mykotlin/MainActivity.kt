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
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OpenCVLoader.initDebug()


//        val CDD = CardDetector(mk.ndarray(mk[170,140]),mk.ndarray(mk[160,200]),0.2f)
//        var textView = findViewById<TextView>(R.id.textView)
//        textView.text = CDD.area.toString()

        val a = Mat.zeros(1,4,CvType.CV_8U)
        val b = Mat()
        a.put(0,0,1.0)
        a.put(0,1,3.0)
        a.put(0,2,5.0)
        a.put(0,3,7.0)
        Core.repeat(a,3,1,b)
        println(a)
        println(b)
        println(b.get(0,0).size)
        for(i in 0..2){
            for(j in 0..3){
                println("$i, $j, ${b.get(i,j)[0]}")
            }
        }



    }

}