package com.example.smartcalender

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.mlkit.vision.common.InputImage
import io.reactivex.subjects.SingleSubject

fun <T> ListenableFuture<T>.toSingle(): SingleSubject<T> {
    val single = SingleSubject.create<T>()
    addListener({ single.onSuccess(get()) }, MoreExecutors.directExecutor())
    return single
}


val ImageProxy.inputImage: InputImage
    @SuppressLint("UnsafeOptInUsageError")
    get() = InputImage.fromMediaImage(image!!, imageInfo.rotationDegrees)
