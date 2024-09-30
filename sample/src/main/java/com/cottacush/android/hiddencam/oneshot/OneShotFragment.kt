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
package com.cottacush.android.hiddencam.oneshot

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cottacush.android.databinding.FragmentOneshotBinding
import com.cottacush.android.hiddencam.HiddenCam
import com.cottacush.android.hiddencam.OnImageCapturedListener
import java.io.File

class OneShotFragment : Fragment(), OnImageCapturedListener {

    private var _binding: FragmentOneshotBinding? = null
    private val binding get() = _binding!!

    private lateinit var hiddenCam: HiddenCam
    private lateinit var baseStorageFolder: File

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOneshotBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        baseStorageFolder = File(requireContext().getExternalFilesDir(null), "HiddenCam").apply {
            if (exists()) deleteRecursively()
            mkdir()
        }
        hiddenCam = HiddenCam(
            requireContext(), baseStorageFolder, this,
            targetResolution = Size(1080, 1920)
        )

        binding.captureButton.setOnClickListener {
            hiddenCam.captureImage()
        }
    }

    override fun onStart() {
        super.onStart()
        hiddenCam.start()
    }

    override fun onImageCaptured(imageUri: Uri?) {
        val message = "Image captured, saved to:${imageUri.toString()}"
        log(message)
        showToast(message)
    }

    override fun onImageCaptureError(e: Throwable?) {
        e?.run {
            val message = "Image captured failed:${e.message}"
            showToast(message)
            log(message)
            printStackTrace()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    override fun onStop() {
        super.onStop()
        hiddenCam.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hiddenCam.destroy()
        _binding = null
    }

    companion object {
        const val TAG = "OneShotFragment"
    }
}
