package com.example.yourstory.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.yourstory.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        //TODO: Setie weiter ausgestallten (root_preferences.xml)
    }
}