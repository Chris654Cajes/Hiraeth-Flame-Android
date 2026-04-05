package com.hiraeth.flame

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.hiraeth.flame.databinding.ActivityMainBinding
import com.hiraeth.flame.ui.util.AppPermissions

/**
 * Hosts [androidx.navigation.fragment.NavHostFragment] and bottom navigation; XML layout is [R.layout.activity_main].
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* results — fragments re-check in onResume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        val topLevel = setOf(
            R.id.libraryFragment,
            R.id.cameraFragment,
            R.id.albumsFragment,
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = topLevel.contains(destination.id)
        }

        requestAppPermissions()
    }

    fun requestAppPermissions() {
        permissionLauncher.launch(AppPermissions.requiredPermissions())
    }
}
