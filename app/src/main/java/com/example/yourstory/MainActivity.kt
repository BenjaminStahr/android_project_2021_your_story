package com.example.yourstory


import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.yourstory.databinding.ActivityMainBinding
import com.example.yourstory.notification.NotificationWorker
import com.example.yourstory.today.thought.SharedThoughtDialogViewModel
import com.example.yourstory.utils.Constants
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit
import android.view.KeyEvent


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var sharedThoughtDialogViewModel: SharedThoughtDialogViewModel
    lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var hostFramentNavController: NavController
    var notificationEnabled = false
    var notificationInterval = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Setup Methods
        initNotifications()
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.customToolbar)

        bottomNavigationView  = binding.bottomNavigation

        hostFramentNavController = findNavController(R.id.host_fragment)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_reports, R.id.navigation_today, R.id.navigation_diary
            )
        )

        setupActionBarWithNavController(hostFramentNavController,appBarConfiguration)
        bottomNavigationView.setupWithNavController(hostFramentNavController)

        //Navigate based on the clicked Navigation button
        if (intent.hasExtra("notification_intent")){
            val intentNumber = intent.extras!!.get("notification_intent")
            if(intentNumber == Constants.NOTIFICATION_ENTRY_CLICKED_CODE){
                hostFramentNavController.navigate(R.id.thought_dialog)
                intent.putExtra("notification_intent",0)
            }
            if(intentNumber == Constants.NOTIFICATION_LIKERT_CLICKED_CODE){
                hostFramentNavController.navigate(R.id.likertDialog)
                intent.putExtra("notification_intent",0)
            }
        }
        sharedThoughtDialogViewModel = ViewModelProvider(this)[SharedThoughtDialogViewModel::class.java]

        hostFramentNavController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.id != R.id.takePictureFragment){
                showBottomNav()
            }
        }
    }



    fun showBottomNav() {
        bottomNavigationView.visibility = View.VISIBLE
        supportActionBar!!.show()
    }

    fun hideBottomNav() {
        bottomNavigationView.visibility = View.GONE
        supportActionBar!!.hide()
    }

    private fun initNotifications() {


        notificationInterval = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .getInt("interval_notification", 12)

        notificationEnabled = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .getBoolean("notification", false)


        if(!notificationEnabled){
            return
        }
        val notificationRequest = PeriodicWorkRequestBuilder<NotificationWorker>(notificationInterval.toLong(),TimeUnit.HOURS)
            .addTag(Constants.NOTIFICATION_ID.toString())
            .setInitialDelay(notificationInterval.toLong(),TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(applicationContext).
        enqueueUniquePeriodicWork("notifications",ExistingPeriodicWorkPolicy.REPLACE,notificationRequest)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_settings_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        16908332 -> {
            //Manipulate Backpress in TakePictureFragment
            if(hostFramentNavController.currentDestination!!.id == R.id.takePictureFragment && hostFramentNavController.previousBackStackEntry!!.destination.id == R.id.takePictureFragment){
                sharedThoughtDialogViewModel.isInCaptureMode = true
                hostFramentNavController.navigate(R.id.action_takePictureFragment_self)
            }
            if(hostFramentNavController.currentDestination!!.id == R.id.recordAudioFragment) {
                val viewModelShared = ViewModelProvider(this)[SharedThoughtDialogViewModel::class.java]
                viewModelShared.clearAudioData()
            }
            onBackPressed()
            true
        }
        R.id.action_settings -> {
            // User chose the "Settings" item, show the app settings UI...
            hostFramentNavController.navigate(R.id.action_global_settingsFragment)
            true
        }

        R.id.action_help -> {
            hostFramentNavController.navigate(R.id.action_global_helpFragment)
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //Manipulate Backpress in TakePictureFragment
            if(hostFramentNavController.currentDestination!!.id == R.id.takePictureFragment && hostFramentNavController.previousBackStackEntry!!.destination.id == R.id.takePictureFragment){
                sharedThoughtDialogViewModel.isInCaptureMode = true
                hostFramentNavController.navigate(R.id.action_takePictureFragment_self)
            }
            if(hostFramentNavController.currentDestination!!.id == R.id.recordAudioFragment) {
                val viewModelShared = ViewModelProvider(this)[SharedThoughtDialogViewModel::class.java]
                viewModelShared.clearAudioData()
            }
            onBackPressed()
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(applicationContext).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        PreferenceManager.getDefaultSharedPreferences(applicationContext).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "notification"){
            if(!sharedPreferences!!.getBoolean("notification",false)) {
                // Destroy Worker
                WorkManager.getInstance(applicationContext)
                    .cancelAllWorkByTag(Constants.NOTIFICATION_ID.toString())
            }else{
                // Init Worker
                initNotifications()
            }
        }
        if(key == "interval_notification"){
            //Interval changed
            initNotifications()
        }
    }
}