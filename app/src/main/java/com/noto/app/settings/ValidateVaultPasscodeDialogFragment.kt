package com.noto.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.noto.app.BaseDialogFragment
import com.noto.app.R
import com.noto.app.databinding.ValidateVaultPasscodeDialogFragmentBinding
import com.noto.app.util.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

class ValidateVaultPasscodeDialogFragment : BaseDialogFragment() {

    private val viewModel by viewModel<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ValidateVaultPasscodeDialogFragmentBinding.inflate(inflater, container, false).withBinding {
        setupState()
        setupListeners()
    }

    private fun ValidateVaultPasscodeDialogFragmentBinding.setupState() {
        tb.tvDialogTitle.text = context?.stringResource(R.string.enter_vault_passcode)

        viewModel.isBioAuthEnabled
            .onEach { isBioAuthEnabled ->
                if (isBioAuthEnabled) {
                    openVaultUsingBio()
                    btnOpenUsingPasscode.text = context?.stringResource(R.string.open_vault_using_passcode)
                    btnOpenUsingBio.isVisible = true
                } else {
                    btnOpenUsingPasscode.text = context?.stringResource(R.string.open_vault)
                    btnOpenUsingBio.isVisible = false
                    activity?.showKeyboard(et)
                }
            }
            .launchIn(lifecycleScope)

        nsv.isScrollingAsFlow()
            .onEach { isScrolling -> tb.ll.isSelected = isScrolling }
            .launchIn(lifecycleScope)
    }

    private fun ValidateVaultPasscodeDialogFragmentBinding.setupListeners() {
        et.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                openVaultUsingPasscode()
                true
            } else {
                false
            }
        }

        btnOpenUsingPasscode.setOnClickListener {
            openVaultUsingPasscode()
        }

        btnOpenUsingBio.setOnClickListener {
            openVaultUsingBio()
        }
    }

    private fun ValidateVaultPasscodeDialogFragmentBinding.openVaultUsingPasscode() {
        val passcode = et.text.toString()
        when {
            passcode.isBlank() -> til.error = context?.stringResource(R.string.passcode_empty_message)
            passcode.hash() == viewModel.vaultPasscode.value -> openVault()
            else -> til.error = context?.stringResource(R.string.invalid_passcode)
        }
    }

    private fun ValidateVaultPasscodeDialogFragmentBinding.openVaultUsingBio() {
        context?.let { context ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.stringResource(R.string.open_vault))
                .setNegativeButtonText(context.stringResource(R.string.use_passcode))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()

            val biometricPrompt = BiometricPrompt(
                this@ValidateVaultPasscodeDialogFragment,
                ContextCompat.getMainExecutor(requireContext()),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        openVault()
                    }
                }
            )

            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun ValidateVaultPasscodeDialogFragmentBinding.openVault() {
        activity?.hideKeyboard(et)
        navController?.previousBackStackEntry?.savedStateHandle?.set(Constants.IsVaultOpen, true)
    }
}