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

    private lateinit var toggleButtons: Array<ToggleButton>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        val thicknesses = intent.getFloatArrayExtra("EXTRA_ESTIMATED4THICKNESSES")
            ?: floatArrayOf(0f,0f,0f,0f)
        val matAddr = intent.getLongExtra("EXTRA_RGBMATADDR",0L)
        val leftMode = intent.getBooleanExtra("EXTRA_LEFTMODE",false)
        toggleButtons = arrayOf(
            findViewById(R.id.button1),
            findViewById(R.id.button2),
            findViewById(R.id.button3),
            findViewById(R.id.button4)
        )


        println("MAIN2")

        if (matAddr != 0L){
            println("img mat addr: $matAddr")
            val matImg = Mat(matAddr)
            val bitmapImg = Bitmap.createBitmap(matImg.size().width.toInt(),matImg.size().height.toInt(),Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(matImg,bitmapImg)
            findViewById<ImageView>(R.id.pictureResult).setImageBitmap(bitmapImg)
        } else {println("no image addr")}

        toggleButtons.forEachIndexed { i, toggleButton ->
            toggleButton.textOn = "${getKSUnit((thicknesses[i]))}호"
            toggleButton.setOnClickListener { toggleSwitch(i) }
        }
        toggleButtons[0].toggle()

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

    fun toggleSwitch(i: Int){
        toggleButtons.forEachIndexed{ idx, toggleButton ->
            if (i != idx) toggleButton.isChecked = false
        }
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