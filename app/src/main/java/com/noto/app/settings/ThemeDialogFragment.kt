package com.noto.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.noto.app.BaseDialogFragment
import com.noto.app.MainViewModel
import com.noto.app.R
import com.noto.app.Theme
import com.noto.app.databinding.BaseDialogFragmentBinding
import com.noto.app.databinding.ThemeDialogFragmentBinding
import com.noto.app.util.stringResource
import com.noto.app.util.withBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ThemeDialogFragment : BaseDialogFragment() {

    private val viewModel by sharedViewModel<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ThemeDialogFragmentBinding.inflate(inflater, container, false).withBinding {

        BaseDialogFragmentBinding.bind(root).apply {
            tvDialogTitle.text = resources.stringResource(R.string.theme)
        }

        viewModel.theme
            .onEach {
                when (it) {
                    Theme.System -> rbSystemTheme.isChecked = true
                    Theme.Light -> rbLightTheme.isChecked = true
                    Theme.Dark -> rbDarkTheme.isChecked = true
                }
            }
            .launchIn(lifecycleScope)

        rbSystemTheme.setOnClickListener {
            dismiss()
            viewModel.updateTheme(Theme.System)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        rbLightTheme.setOnClickListener {
            dismiss()
            viewModel.updateTheme(Theme.Light)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        rbDarkTheme.setOnClickListener {
            dismiss()
            viewModel.updateTheme(Theme.Dark)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}