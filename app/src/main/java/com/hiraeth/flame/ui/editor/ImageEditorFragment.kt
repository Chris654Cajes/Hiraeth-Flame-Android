package com.hiraeth.flame.ui.editor

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.hiraeth.flame.R
import com.hiraeth.flame.databinding.FragmentImageEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageEditorFragment : Fragment() {

    private var _binding: FragmentImageEditorBinding? = null
    private val binding get() = _binding!!

    private val container get() = (requireActivity().application as com.hiraeth.flame.HiraethApplication).container

    private val mediaId: Long get() = requireArguments().getLong("mediaId")

    private val viewModel: ImageEditorViewModel by viewModels {
        ImageEditorViewModel.factory(container.mediaRepository, mediaId)
    }

    private var quarterTurns = 0
    private var drawEnabled = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentImageEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = findNavController()
        val appBarConfig = AppBarConfiguration(setOf(R.id.libraryFragment, R.id.cameraFragment, R.id.albumsFragment))
        binding.toolbar.setupWithNavController(navController, appBarConfig)

        viewLifecycleOwner.lifecycleScope.launch {
            val entity = withContext(Dispatchers.IO) { container.mediaRepository.getById(mediaId) }
            if (entity == null || entity.isVideo) {
                binding.statusText.text = "Not an image."
                return@launch
            }
            val file = container.mediaRepository.resolveFile(entity)
            binding.imageView.load(file) { crossfade(true) }
        }

        val brightnessListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val b = 0.4f + (progress / 120f) * 1.2f
                val cm = ColorMatrix()
                cm.setScale(b, b, b, 1f)
                binding.imageView.colorFilter = ColorMatrixColorFilter(cm)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        binding.seekBrightness.setOnSeekBarChangeListener(brightnessListener)
        brightnessListener.onProgressChanged(binding.seekBrightness, binding.seekBrightness.progress, false)

        binding.btnRotate.setOnClickListener {
            quarterTurns = (quarterTurns + 1) % 4
            binding.imageView.rotation = 90f * quarterTurns
        }

        binding.btnToggleDraw.setOnClickListener {
            drawEnabled = !drawEnabled
            binding.drawOverlay.drawingEnabled = drawEnabled
            binding.drawOverlay.visibility = if (drawEnabled) View.VISIBLE else View.GONE
            binding.btnToggleDraw.text = if (drawEnabled) "Draw (on)" else "Draw"
        }

        binding.btnSave.setOnClickListener {
            val bright = 0.4f + (binding.seekBrightness.progress / 120f) * 1.2f
            val contrast = 0.5f + (binding.seekContrast.progress / 130f) * 1.3f
            val sat = binding.seekSaturation.progress / 100f
            viewModel.export(
                brightness = bright,
                contrast = contrast,
                saturation = sat,
                rotationQuarterTurns = quarterTurns,
                cropLeft = 0f,
                cropTop = 0f,
                cropRight = 1f,
                cropBottom = 1f,
                strokes = binding.drawOverlay.snapshotStrokes(),
            ) { ok ->
                if (ok) findNavController().popBackStack()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.exporting.collect { exp ->
                        binding.btnSave.isEnabled = !exp
                        binding.btnSave.text = if (exp) "Saving…" else "Apply & save"
                    }
                }
                launch {
                    viewModel.message.collect { binding.statusText.text = it.orEmpty() }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
