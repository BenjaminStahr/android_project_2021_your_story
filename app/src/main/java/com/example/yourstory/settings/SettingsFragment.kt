package com.example.yourstory.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.yourstory.R
import com.example.yourstory.databinding.SettingsFragmentBinding

class SettingsFragment : Fragment() {

    private lateinit var hostFramentNavController: NavController
    private lateinit var binding: SettingsFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentBinding.inflate(inflater,container,false)

        if (container != null) {
            hostFramentNavController = container.findNavController()
        }

        binding.notificationItem.setOnClickListener {
            hostFramentNavController.navigate(R.id.action_settingsFragment_to_settingsNotificationFragment)
        }

        binding.diaryItem.setOnClickListener{
            hostFramentNavController.navigate(R.id.action_settingsFragment_to_settingsFragmentBackup)
        }

        return binding.root
    }
}