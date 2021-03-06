package com.example.yourstory.settings

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.yourstory.R
import com.example.yourstory.databinding.SettingsBackupLoggedInFragmentBinding
import com.example.yourstory.databinding.SettingsBackupNotLoggedInFragmentBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.Scope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.api.services.drive.DriveScopes

class SettingsBackupFragment : Fragment() {


    private  lateinit var client: GoogleSignInClient
    private lateinit var hostFramentNavController: NavController
    private lateinit var viewModel: SettingsFragmentBackupViewModel
    private lateinit var binding_not_logged_in: SettingsBackupNotLoggedInFragmentBinding
    private lateinit var binding_logged_in: SettingsBackupLoggedInFragmentBinding
    lateinit var materialAlertDialogBuilder: MaterialAlertDialogBuilder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[SettingsFragmentBackupViewModel::class.java]


        if (container != null) {
            hostFramentNavController = container.findNavController()
        }

        //Check if a Google Account exists.
        if(viewModel.repository.getGoogleAccount() == null){
            binding_not_logged_in = SettingsBackupNotLoggedInFragmentBinding.inflate(layoutInflater)

            binding_not_logged_in.signInButton.setOnClickListener{
                requestSignIn()
            }

            binding_not_logged_in.signInButton.setSize(SignInButton.SIZE_WIDE)
            (binding_not_logged_in.signInButton.getChildAt(0) as TextView).text = getString(R.string.login_with_google)
            (binding_not_logged_in.signInButton.getChildAt(0) as TextView).setTextColor(Color.GRAY)

            return binding_not_logged_in.root
        }

        binding_logged_in = SettingsBackupLoggedInFragmentBinding.inflate(layoutInflater)
        binding_logged_in.textViewName.text = viewModel.getGoogleDisplayName()
        binding_logged_in.textViewEmail.text = viewModel.getGoogleEmail()

        binding_logged_in.signOutButton.setSize(SignInButton.SIZE_WIDE)
        (binding_logged_in.signOutButton.getChildAt(0) as TextView).text = getString(R.string.logout_google)
        (binding_logged_in.signOutButton.getChildAt(0) as TextView).setTextColor(Color.GRAY)

        //Init Google-SignIn Button
        binding_logged_in.signOutButton.setOnClickListener{
            signOut()
        }

        //Init upload Button
        binding_logged_in.buttonUpload.setOnClickListener{
            viewModel.uploadDataBase()
        }

        binding_logged_in.buttonDownload.setOnClickListener{
            viewModel.downloadDatabase()
        }

        //Init observing if a backup exists
        viewModel.latestDBMetadata.observe(viewLifecycleOwner,{
            if(it == null){
                binding_logged_in.textViewBackupDate.text = resources.getString(R.string.settings_backup_no_backup_found)
                binding_logged_in.buttonDownload.isEnabled = false;
            }else{
                binding_logged_in.textViewBackupDate.text = it.createdTime.toString().split("T")[0] + "  " + it.createdTime.toString().split("T")[1].split(".")[0]
                binding_logged_in.buttonDownload.isEnabled = true;
            }
        })

        viewModel.latestDBFile.observe(viewLifecycleOwner,{
            if(it != null){
                materialAlertDialogBuilder.setTitle(resources.getString(R.string.settings_backup_download_success))
                materialAlertDialogBuilder.setMessage(resources.getString(R.string.settings_backup_download_success_text))
                materialAlertDialogBuilder.setPositiveButton(resources.getString(R.string.thought_empty_dialog_ok_button)){
                        _, _ ->
                }
                materialAlertDialogBuilder.show()
            }
        })

        return binding_logged_in.root
    }

    private fun signOut() {
        GoogleSignIn.getClient(requireActivity(),GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE))
            .requestEmail()
            .build()).signOut().addOnCompleteListener(requireActivity()) {
            viewModel.repository.signOutFromGoogle()
        }
        hostFramentNavController.navigate(R.id.settingsFragmentBackup)
    }

    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE))
            .build()
        client = GoogleSignIn.getClient(requireActivity(), signInOptions)

        startActivityForResult(client.signInIntent,400)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            400 -> {
                if(resultCode == RESULT_OK) {
                    handleSignInIntent(data)
                }
            }
        }
    }

    private fun handleSignInIntent(data: Intent?) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener {

                viewModel.setGoogleAccountAndInitDrive(it)
                hostFramentNavController.navigate(R.id.settingsFragmentBackup)
            }
            .addOnFailureListener{
                this.requireView().invalidate()
            }
    }
}