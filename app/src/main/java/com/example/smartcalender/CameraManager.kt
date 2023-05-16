package com.example.smartcalender

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject

object CameraManager {

    var preview: Preview? = null

    fun initCamera(context: Context): Single<ProcessCameraProvider> {
        return ProcessCameraProvider.getInstance(context).toSingle()
    }

    fun runCamera(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        surface: SurfaceProvider
    ) {

        preview = Preview.Builder().build()

        val cameraSelector: CameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview?.setSurfaceProvider(surface)

        cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, preview
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun getInputImageFlow(
        activity: Activity,
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner
    ): Single<ImageProxy> {
        val publishSubject = SingleSubject.create<ImageProxy>()
        val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(activity)) { imageProxy ->
            publishSubject.onSuccess(imageProxy)
            imageAnalysis.clearAnalyzer()
            cameraProvider.unbind(imageAnalysis)//review if this needed
        }

        val cameraSelector: CameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, preview, imageAnalysis
        )

        return publishSubject
    }
}