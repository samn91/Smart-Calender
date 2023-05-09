package com.example.smartcalender

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.example.smartcalender.databinding.FragmentMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var lastImage = System.currentTimeMillis()
    private var disposable: Disposable? = null
    private var cameraProvider: ProcessCameraProvider? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            cameraProvider?.unbindAll()
            initCamera(cameraProvider)
        }
        val permissions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )

        val askForPermission = permissions.any {
            ActivityCompat.checkSelfPermission(
                requireActivity(),
                it
            ) != PackageManager.PERMISSION_DENIED
        }

        if (askForPermission) {
            ActivityCompat.requestPermissions(
                requireActivity(), permissions,
                1241234
            )
            // return
        }

//        val future = Executors.newSingleThreadExecutor().submit {
//            val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get()
//            initCamera(cameraProvider)
//        }
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext())
        cameraProvider.addListener(
            { initCamera(cameraProvider.get()) }, requireContext().mainExecutor
        )
    }


    @SuppressLint("UnsafeOptInUsageError")
    private fun initCamera(cameraProvider: ProcessCameraProvider?) {
        this.cameraProvider = cameraProvider ?: return
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->

            if (imageProxy.image == null || System.currentTimeMillis() - lastImage <= 1000) {
                imageProxy.close()
                return@setAnalyzer
            }

            disposable = processText(imageProxy)
                .doFinally {
                    imageProxy.close()
                    imageAnalysis.clearAnalyzer()
                    cameraProvider.unbindAll()
                }
                .flatMap { chatGptApi.chat(body = RequestBody.createRequestBody(it)) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("ChatResponse", it.toString())
                    lastImage = System.currentTimeMillis()
                    binding.textviewFirst.text = it.choices.firstOrNull()?.message?.getCalenderEvent()?.toString()
                }, {
                    context ?: return@subscribe
                    Toast.makeText(context, "Failed to get text: ${it.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ChatResponse","error", it)
                })
        }
        val preview: Preview = Preview.Builder().build()

        val cameraSelector: CameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview.setSurfaceProvider(binding.pvMain.surfaceProvider)

        var camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner, cameraSelector, imageAnalysis, preview
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processText(imageProxy: ImageProxy): SingleSubject<String> {
        val mediaImage = imageProxy.image
        val image =
            InputImage.fromMediaImage(mediaImage!!, imageProxy.imageInfo.rotationDegrees)

        val publishSubject = SingleSubject.create<String>()

        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image).addOnSuccessListener {
                publishSubject.onSuccess(it.text)
            }.addOnFailureListener {
                publishSubject.onError(it)
            }
        return publishSubject
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        disposable?.dispose()
        cameraProvider = null
    }
}
