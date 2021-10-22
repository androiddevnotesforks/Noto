package com.noto.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.noto.app.AppViewModel
import com.noto.app.BaseDialogFragment
import com.noto.app.R
import com.noto.app.databinding.BaseDialogFragmentBinding
import com.noto.app.databinding.LanguageDialogFragmentBinding
import com.noto.app.domain.model.Language
import com.noto.app.util.stringResource
import com.noto.app.util.withBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

class LanguageDialogFragment : BaseDialogFragment() {

    private val viewModel by viewModel<AppViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = LanguageDialogFragmentBinding.inflate(inflater, container, false).withBinding {
        setupBaseDialogFragment()
        setupState()
        setupListeners()
    }

    fun LanguageDialogFragmentBinding.setupBaseDialogFragment() = BaseDialogFragmentBinding.bind(root).apply {
        context?.let { context ->
            tvDialogTitle.text = context.stringResource(R.string.language)
        }
    }

    fun LanguageDialogFragmentBinding.setupState() {
        viewModel.language
            .onEach { language ->
                when (language) {
                    Language.System -> rbSystem.isChecked = true
                    Language.English -> rbEnglish.isChecked = true
                    Language.Turkish -> rbTurkish.isChecked = true
                }
            }
            .launchIn(lifecycleScope)
    }

    fun LanguageDialogFragmentBinding.setupListeners() {
        rbSystem.setOnClickListener {
            dismiss()
            viewModel.updateLanguage(Language.System)
        }

        rbEnglish.setOnClickListener {
            dismiss()
            viewModel.updateLanguage(Language.English)
        }

        rbTurkish.setOnClickListener {
            dismiss()
            viewModel.updateLanguage(Language.Turkish)
        }
    }
}