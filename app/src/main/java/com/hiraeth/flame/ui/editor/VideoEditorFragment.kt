package com.hiraeth.flame.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.hiraeth.flame.R
import com.hiraeth.flame.databinding.FragmentVideoEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class VideoEditorFragment : Fragment() {

    private var _binding: FragmentVideoEditorBinding? = null
    private val binding get() = _binding!!

    private val container get() = (requireActivity().application as com.hiraeth.flame.HiraethApplication).container

    private val mediaId: Long get() = requireArguments().getLong("mediaId")

    private val viewModel: VideoEditorViewModel by viewModels {
        VideoEditorViewModel.factory(container.mediaRepository, container.mediaStorage, mediaId)
    }

    private var player: ExoPlayer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVideoEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = findNavController()
        val appBarConfig = AppBarConfiguration(setOf(R.id.libraryFragment, R.id.cameraFragment, R.id.albumsFragment))
        binding.toolbar.setupWithNavController(navController, appBarConfig)

        viewLifecycleOwner.lifecycleScope.launch {
            val entity = withContext(Dispatchers.IO) { container.mediaRepository.getById(mediaId) }
            if (entity == null || !entity.isVideo) {
                binding.statusText.text = "Not a video."
                return@launch
            }
            val file = container.mediaRepository.resolveFile(entity)
            player = ExoPlayer.Builder(requireContext()).build().apply {
                setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
                prepare()
                playWhenReady = false
            }
            binding.playerView.player = player
        }

        val sync: () -> Unit = {
            var start = binding.seekStart.progress / 100f
            var end = binding.seekEnd.progress / 100f
            if (end <= start + 0.02f) {
                end = (start + 0.05f).coerceAtMost(1f)
                binding.seekEnd.progress = (end * 100).toInt()
            }
        }
        binding.seekStart.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) { sync() }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        binding.seekEnd.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) { sync() }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnExport.setOnClickListener {
            val start = binding.seekStart.progress / 100f
            val end = binding.seekEnd.progress / 100f
            viewModel.exportTrim(start, end) { newId ->
                findNavController().navigate(
                    R.id.mediaDetailFragment,
                    bundleOf("mediaId" to newId),
                    NavOptions.Builder()
                        .setPopUpTo(R.id.libraryFragment, false)
                        .setLaunchSingleTop(true)
                        .build(),
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.busy.collect { busy ->
                        binding.btnExport.isEnabled = !busy
                        binding.btnExport.text = if (busy) "Working…" else "Export trim as new file"
                    }
                }
                launch {
                    viewModel.status.collect { binding.statusText.text = it.orEmpty() }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playerView.player = null
        player?.release()
        player = null
        _binding = null
    }
}
