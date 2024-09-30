/**
 * Copyright (c) 2019 Cotta & Cush Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cottacush.android.hiddencam

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.cottacush.android.hiddencam.CaptureTimeFrequency.OneShot
import com.cottacush.android.hiddencam.CaptureTimeFrequency.Recurring
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class HiddenCam @JvmOverloads constructor(
    context: Context,
    private val baseFileDirectory: File,
    private val imageCapturedListener: OnImageCapturedListener,
    private val captureFrequency: CaptureTimeFrequency = OneShot,
    private val targetAspectRatio: TargetAspectRatio? = null,
    private val targetResolution: Size? = null,
    private val targetRotation: Int? = null,
    private val cameraType: CameraType = CameraType.FRONT_CAMERA
) {
    private lateinit var captureTimer: CaptureTimerHandler
    private val lifeCycleOwner = HiddenCamLifeCycleOwner()

    private lateinit var mContext: Context


    /**
     * For some devices, if the camera doesn't have time to preview before the actual capture, it
     * would result into an underexposed or overexposed image. Hence, A [Preview] use case is set up
     * which renders to a dummy surface.
     */
//    private var preview: Preview

    /** Configures the camera for preview */
//    private var previewConfig = Preview.Builder().apply {
//        setLensFacing(cameraType.lensFacing)
//        if (targetRotation != null) setTargetRotation(targetRotation)
//        if (targetAspectRatio != null) setTargetAspectRatio(targetAspectRatio.aspectRatio)
//        if (targetResolution != null) setTargetResolution(targetResolution)
//    }.build()


    /**
     * An [ImageCapture] use case to capture images.
     */
    private lateinit var imageCapture: ImageCapture

    /**
     * Configures the camera for Image Capture.
     */
//    private var imageCaptureConfig: ImageCaptureConfig = ImageCaptureConfig.Builder()
//        .apply {
//            setLensFacing(cameraType.lensFacing)
//            if (targetRotation != null) setTargetRotation(targetRotation)
//            if (targetResolution != null) setTargetResolution(targetResolution)
//            if (targetAspectRatio != null) setTargetAspectRatio(targetAspectRatio.aspectRatio)
//        }.build()

    /**
     * Provides basic setup for the camera engine.
     * throws SecurityException if the required permissions aren't available.
     */
    init {
        mContext = context;
        if (context.hasPermissions()) {

            val previewBuilder = Preview.Builder()
            if (targetRotation != null) {
                previewBuilder.setTargetRotation(targetRotation)
            }
            if (targetResolution != null) {
                previewBuilder.setTargetResolution(targetResolution)
            }
            if (targetAspectRatio != null) {
                previewBuilder.setTargetAspectRatio(targetAspectRatio.aspectRatio)
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({

                // Camera provider is now guaranteed to be available
                val cameraProvider = cameraProviderFuture.get()

                // Set up the view finder use case to display camera preview
                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                    .setTargetRotation(activityCameraBinding.viewFinder.display.rotation)
                    .build()


                // Create a new camera selector each time, enforcing lens facing
                val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraType.lensFacing).build()

                imageCapture = ImageCapture.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()

                // Apply declared configs to CameraX using the same lifecycle owner
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifeCycleOwner, cameraSelector, preview, imageCapture)

                // Use the camera object to link our preview use case with the view
//                preview.setSurfaceProvider(activityCameraBinding.viewFinder.surfaceProvider)

            }, ContextCompat.getMainExecutor(context))

            when (val interval = captureFrequency) {
                OneShot -> {
                    // Nothing for now, we don't need to schedule anything
                }
                is Recurring -> {
                    captureTimer = CaptureTimerHandler(
                        interval.captureIntervalMillis,
                        object : CaptureTimeListener {
                            override fun onCaptureTimeTick() {
                                capture()
                            }
                        })
                }
            }
        } else throw SecurityException("You need to have access to both CAMERA and WRITE_EXTERNAL_STORAGE permissions")
    }

    /**
     * Mark [HiddenCam]'s lifecycle as started. If the [CaptureTimeFrequency] supplied is
     * [Recurring], automatic image captures are triggered.
     */
    fun start() {
        lifeCycleOwner.start()
        if (captureFrequency is Recurring) captureTimer.startUpdates()
    }

    /**
     * Mark [HiddenCam]'s lifecycle as stopped. If the [CaptureTimeFrequency] supplied is
     * [Recurring], automatic image captures are stopped.
     */
    fun stop() {
        lifeCycleOwner.stop()
        if (captureFrequency is Recurring) captureTimer.stopUpdates()
    }

    /**
     * Mark [HiddenCam]'s lifecycle as destroyed. If the kind of [CaptureTimeFrequency] supplied is
     * [Recurring], Automatic image captures are stopped.
     */
    fun destroy() {
        lifeCycleOwner.tearDown()
        if (captureFrequency is Recurring) captureTimer.stopUpdates()
    }

    /**
     * Method to manually trigger an image capture.
     * @see capture
     */
    fun captureImage() {
        if (captureFrequency is OneShot) {
            capture()
        }
    }

    /**
     * Capture image and save in the container directory supplied.
     * The results are delivered with the supplied [OnImageCapturedListener]
     */
    private fun capture() {

        val name = SimpleDateFormat(FILENAME, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
            val appName = mContext.resources.getString(R.string.app_name)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(mContext.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        imageCapture.takePicture(
            outputOptions, MainThreadExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                }
            })

//        imageCapture.takePicture(createFile(baseFileDirectory), MainThreadExecutor,
//            object : ImageCapture.OnImageSavedListener {
//                override fun onError(
//                    imageCaptureError: ImageCapture.ImageCaptureError,
//                    message: String,
//                    cause: Throwable?
//                ) = imageCapturedListener.onImageCaptureError(cause)
//
//                override fun onImageSaved(file: File) = imageCapturedListener.onImageCaptured(file)
//            })
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_TYPE = "image/jpeg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
