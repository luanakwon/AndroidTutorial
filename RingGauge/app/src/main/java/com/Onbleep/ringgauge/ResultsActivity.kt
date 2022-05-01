package com.onbleep.ringgauge

import android.graphics.Bitmap
import android.media.Image
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import org.opencv.android.Utils
import org.opencv.core.Mat

class ResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        val thicknesses = intent.getFloatArrayExtra("EXTRA_ESTIMATED4THICKNESSES")
            ?: floatArrayOf(0f,0f,0f,0f)
        val matAddr = intent.getLongExtra("EXTRA_RGBMATADDR",0L)
        val leftMode = intent.getBooleanExtra("EXTRA_LEFTMODE",false)

        println("MAIN2")

        if (matAddr != 0L){
            println("img mat addr: $matAddr")
            val matImg = Mat(matAddr)
            val bitmapImg = Bitmap.createBitmap(matImg.size().width.toInt(),matImg.size().height.toInt(),Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(matImg,bitmapImg)
            findViewById<ImageView>(R.id.pictureResult).setImageBitmap(bitmapImg)
        } else {println("no image addr")}

        findViewById<ToggleButton>(R.id.button1).textOn = "${getKSUnit(thicknesses[0])} 호"
        findViewById<ToggleButton>(R.id.button2).textOn = "${getKSUnit(thicknesses[1])} 호"
        findViewById<ToggleButton>(R.id.button3).textOn = "${getKSUnit(thicknesses[2])} 호"
        findViewById<ToggleButton>(R.id.button4).textOn = "${getKSUnit(thicknesses[3])} 호"

        val resultsTable: LinearLayout = findViewById(R.id.resultsTable)
        val closeTableBtn: ImageView = findViewById(R.id.closeTableBtn)
        val openTableBtn: ImageView = findViewById(R.id.openTableBtn)
        closeTableBtn.setOnClickListener {
            resultsTable.visibility = View.INVISIBLE
            openTableBtn.visibility = View.VISIBLE
        }
        openTableBtn.setOnClickListener {
            resultsTable.visibility = View.VISIBLE
            openTableBtn.visibility = View.INVISIBLE
        }
        findViewById<Button>(R.id.button5).setOnClickListener {
            this@ResultsActivity.finish()
        }
        // change results table position
        val pLayout: ConstraintLayout = findViewById(R.id.parentCTLayout2)
        val set = ConstraintSet()
        set.clone(pLayout)
        if (leftMode) {
            Log.i("ResultsAct","left")
            set.clear(R.id.resultsTable,ConstraintSet.END)
            set.connect(R.id.resultsTable,ConstraintSet.START,R.id.parentCTLayout2,ConstraintSet.START,40)
            set.clear(R.id.openTableBtn,ConstraintSet.END)
            set.connect(R.id.openTableBtn,ConstraintSet.START,R.id.parentCTLayout2,ConstraintSet.START,10)
        } else {
            Log.i("ResultsAct","right")
            set.clear(R.id.resultsTable,ConstraintSet.START)
            set.connect(R.id.resultsTable,ConstraintSet.END,R.id.parentCTLayout2,ConstraintSet.END,40)
            set.clear(R.id.openTableBtn,ConstraintSet.START)
            set.connect(R.id.openTableBtn,ConstraintSet.END,R.id.parentCTLayout2,ConstraintSet.END,10)
        }
        set.applyTo(pLayout)
    }

    fun getKSUnit(x: Float) : Int{
        return ((x-13f)*3 + 1.5).toInt()
    }

    var mBackWait: Long = 0
    override fun onBackPressed() {
        if (System.currentTimeMillis() - mBackWait >= 2000){
            mBackWait = System.currentTimeMillis()
            Toast.makeText(
                this, "뒤로가기 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT)
                .show()
        } else {
            finishAffinity()
        }
    }
}