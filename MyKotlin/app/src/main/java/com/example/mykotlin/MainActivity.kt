package com.example.mykotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import org.jetbrains.kotlinx.multik.ndarray.operations.filter
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OpenCVLoader.initDebug()

        val a = mk.ndarray(mk[mk[1,2],mk[3,4],mk[5,6]]).asType<Float>()
        val b = mk.ndarray(mk[mk[-1,-2]])
        val c = mk.ndarray(mk[mk[1],mk[2],mk[3]])
        println(a / 1f)
        println(1f / a)
        println(mk.math.argMax(a))


        val CDD = CardDetector(mk.ndarray(mk[0,0]),mk.ndarray(mk[2,1]),0.2f)
        var textView = findViewById<TextView>(R.id.textView)
        textView.text = CDD.area.toString()


    }

}