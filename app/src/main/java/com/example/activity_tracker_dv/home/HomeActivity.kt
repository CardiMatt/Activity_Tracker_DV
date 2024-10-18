package com.example.activity_tracker_dv.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.activity_tracker_dv.R
import com.example.activity_tracker_dv.home.fragment.ActivityFragment
import com.example.activity_tracker_dv.home.fragment.SettingsFragment
import com.example.activity_tracker_dv.home.fragment.StatisticsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var navView: BottomNavigationView
    private val activityFragment = ActivityFragment()
    private val statisticsFragment = StatisticsFragment()
    private val settingsFragment = SettingsFragment()
    private var activeFragment: Fragment = activityFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        navView = findViewById(R.id.nav_view)

        // Aggiungi tutti i fragment e nascondi quelli che non devono essere mostrati inizialmente
        supportFragmentManager.beginTransaction().apply {
            add(R.id.nav_host_fragment_activity_main, settingsFragment, "3").hide(settingsFragment)
            add(R.id.nav_host_fragment_activity_main, statisticsFragment, "2").hide(statisticsFragment)
            add(R.id.nav_host_fragment_activity_main, activityFragment, "1")
        }.commit()

        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_activity -> {
                    switchFragment(activityFragment)
                    true
                }
                R.id.navigation_statistics -> {
                    switchFragment(statisticsFragment)
                    true
                }
                R.id.navigation_settings -> {
                    switchFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit()
        activeFragment = fragment
    }
}
