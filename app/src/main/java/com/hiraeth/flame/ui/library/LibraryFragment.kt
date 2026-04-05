package com.hiraeth.flame.ui.library

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiraeth.flame.R
import com.hiraeth.flame.databinding.FragmentLibraryBinding
import com.hiraeth.flame.domain.LibrarySort
import com.hiraeth.flame.domain.LibraryViewMode
import com.hiraeth.flame.domain.MediaTypeFilter
import com.hiraeth.flame.ui.util.AppPermissions
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val container get() = (requireActivity().application as com.hiraeth.flame.HiraethApplication).container

    private val viewModel: LibraryViewModel by viewModels {
        LibraryViewModel.factory(container.mediaRepository)
    }

    private lateinit var adapter: MediaLibraryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = MediaLibraryAdapter(
            container = container,
            gridMode = viewModel.viewModeState.value == LibraryViewMode.Grid,
            onItemClick = { id ->
                val b = Bundle().apply { putLong("mediaId", id) }
                findNavController().navigate(R.id.action_library_to_detail, b)
            },
        )
        binding.recycler.adapter = adapter
        applyLayoutManager()

        val sortLabels = LibrarySort.entries.map { it.name.replace(Regex("([a-z])([A-Z])"), "$1 $2") }
        binding.sortSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortLabels)
        binding.sortSpinner.setSelection(LibrarySort.entries.indexOf(LibrarySort.DateNewest))
        binding.sortSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                viewModel.setSort(LibrarySort.entries[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.tagFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setTagFilter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.filterAll.setOnClickListener { viewModel.setTypeFilter(MediaTypeFilter.All) }
        binding.filterPhotos.setOnClickListener { viewModel.setTypeFilter(MediaTypeFilter.ImagesOnly) }
        binding.filterVideos.setOnClickListener { viewModel.setTypeFilter(MediaTypeFilter.VideosOnly) }

        binding.fabImport.setOnClickListener {
            if (hasAllPermissions()) {
                findNavController().navigate(R.id.action_library_to_import)
            } else {
                (activity as? com.hiraeth.flame.MainActivity)?.requestAppPermissions()
            }
        }

        binding.btnGrantPermissions.setOnClickListener {
            (activity as? com.hiraeth.flame.MainActivity)?.requestAppPermissions()
        }

        val navController = findNavController()
        val appBarConfig = AppBarConfiguration(
            setOf(R.id.libraryFragment, R.id.cameraFragment, R.id.albumsFragment),
        )
        binding.toolbar.setupWithNavController(navController, appBarConfig)
        binding.toolbar.inflateMenu(R.menu.menu_library)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_toggle_view -> {
                    viewModel.toggleViewMode()
                    true
                }
                R.id.action_reel -> {
                    findNavController().navigate(R.id.action_library_to_reel)
                    true
                }
                else -> false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.items.collect { adapter.submitList(it) }
                }
                launch {
                    viewModel.viewModeState.collect { mode ->
                        val grid = mode == LibraryViewMode.Grid
                        adapter.setGridMode(grid)
                        applyLayoutManager()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                updatePermissionUi()
            }
        }
    }

    private fun applyLayoutManager() {
        val grid = viewModel.viewModeState.value == LibraryViewMode.Grid
        binding.recycler.layoutManager = if (grid) {
            GridLayoutManager(requireContext(), 3)
        } else {
            LinearLayoutManager(requireContext())
        }
    }

    private fun hasAllPermissions(): Boolean =
        AppPermissions.requiredPermissions().all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

    private fun updatePermissionUi() {
        val ok = hasAllPermissions()
        binding.btnGrantPermissions.visibility = if (ok) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
