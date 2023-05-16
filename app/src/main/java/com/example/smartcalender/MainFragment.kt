package com.example.smartcalender

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.smartcalender.databinding.FragmentMainBinding
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
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
            captureEvent(cameraProvider)
        }
        val permissions = arrayOf(
            android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_CALENDAR
        )

        val askForPermission = permissions.any {
            ActivityCompat.checkSelfPermission(
                requireActivity(), it
            ) != PackageManager.PERMISSION_DENIED
        }

        if (askForPermission) {
            ActivityCompat.requestPermissions(
                requireActivity(), permissions, 1241234
            )
            // return
        }

        CameraManager.initCamera(requireContext()).observeOn(AndroidSchedulers.mainThread())
            .subscribe { it, error ->
                cameraProvider = it
                CameraManager.runCamera(it, viewLifecycleOwner, binding.pvMain.surfaceProvider)
            }
    }

    private fun captureEvent(cameraProvider: ProcessCameraProvider?) {
        cameraProvider ?: return
        disposable?.dispose()
        disposable = Single.just(true)//to make the retry trigger CameraManager
            .flatMap {
                CameraManager.getInputImageFlow(
                    requireActivity(), cameraProvider, viewLifecycleOwner
                )
            }.flatMap { processText(it) }.flatMap {
                if (it.isBlank()) {
                    return@flatMap Single.error(Throwable("failed to get text"))
                }
                return@flatMap Single.fromCallable { it }
            }.doOnSuccess { Log.d("ChatResponse", "Text from image= $it") }.retry { count, error ->
                if (count < 2) {
                    Log.e("ChatResponse", "retrying($count) because of error", error)
                    return@retry true
                }
                return@retry false
            }.flatMap { chatGptApi.chat(body = RequestBody.createRequestBody(it)) }
            .map { it.choices.first().message }
            .doOnSuccess { Log.d("ChatResponse", "ChatGPT response= $it") }
            .map { it.getCalenderEvent()!! }.observeOn(AndroidSchedulers.mainThread()).subscribe({
                lastImage = System.currentTimeMillis()
                binding.textviewFirst.text = it.toString()
                CalenderHelper.addEvent(requireActivity(), it)
            }, {
                Log.e("ChatResponse", "error", it)
                context ?: return@subscribe
                Toast.makeText(context, "Failed to get text: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            })
    }

    private fun processText(imageProxy: ImageProxy): SingleSubject<String> {
        val publishSubject = SingleSubject.create<String>()
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(imageProxy.inputImage).addOnSuccessListener {
                publishSubject.onSuccess(it.text)
                imageProxy.close()
            }.addOnFailureListener {
                publishSubject.onError(it)
                imageProxy.close()
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
