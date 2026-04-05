package com.hiraeth.flame.ui.mediaimport

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.hiraeth.flame.R
import com.hiraeth.flame.databinding.FragmentImportBinding
import kotlinx.coroutines.launch

class ImportFragment : Fragment() {

    private var _binding: FragmentImportBinding? = null
    private val binding get() = _binding!!

    private val container get() = (requireActivity().application as com.hiraeth.flame.HiraethApplication).container

    private val viewModel: ImportPreviewViewModel by viewModels {
        ImportPreviewViewModel.factory(container.mediaRepository)
    }

    private var pickedUri: Uri? = null
    private var isVideo: Boolean = false

    private val pickLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedUri = uri
        if (uri != null) {
            val type = requireContext().contentResolver.getType(uri).orEmpty()
            isVideo = type.startsWith("video/")
            binding.preview.load(uri) { crossfade(true) }
            if (binding.titleInput.text.isNullOrBlank()) {
                binding.titleInput.setText(uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.').orEmpty())
            }
            binding.detectLabel.text = if (isVideo) "Detected: video" else "Detected: image"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setupWithNavController(findNavController())

        binding.btnPick.setOnClickListener { pickLauncher.launch("*/*") }

        binding.btnSave.setOnClickListener {
            val uri = pickedUri ?: return@setOnClickListener
            viewModel.import(
                uri,
                binding.titleInput.text?.toString().orEmpty(),
                isVideo,
            ) { id ->
                findNavController().navigate(
                    R.id.action_import_to_detail,
                    bundleOf("mediaId" to id),
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.busy.collect { busy ->
                        binding.progress.visibility = if (busy) View.VISIBLE else View.GONE
                        binding.btnSave.isEnabled = !busy
                    }
                }
                launch {
                    viewModel.error.collect { err ->
                        if (err != null) {
                            binding.errorText.visibility = View.VISIBLE
                            binding.errorText.text = err
                        } else {
                            binding.errorText.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
