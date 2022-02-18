package com.example.mykotlin

import android.content.Context
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import org.opencv.android.Utils
import org.opencv.core.Mat

class FingerDipDetection(
    context:Context,
    static_image_mode:Boolean = true,
    max_num_hands:Int = 1,
    model_complexity:Int = 0,
    min_detection_confidence:Float = 0.5f
){

    val handsOptions = HandsOptions.builder()
        .setStaticImageMode(static_image_mode)
        .setMaxNumHands(max_num_hands)
        .setRunOnGpu(true)
        .setModelComplexity(model_complexity)
        .setMinDetectionConfidence(min_detection_confidence)
        .build()
    val hands = Hands(context,handsOptions)
    init {
        hands.setErrorListener { message, e ->  println(message)} //TODO set error listener
    }

    fun run(img:Mat){
        hands
    }

}