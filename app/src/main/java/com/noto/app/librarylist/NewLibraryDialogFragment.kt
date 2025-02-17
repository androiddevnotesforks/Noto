package com.noto.app.librarylist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.addCallback
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.noto.app.BaseDialogFragment
import com.noto.app.R
import com.noto.app.databinding.BaseDialogFragmentBinding
import com.noto.app.databinding.NewLibraryDialogFragmentBinding
import com.noto.app.domain.model.NotoColor
import com.noto.app.notelist.NoteListViewModel
import com.noto.app.util.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NewLibraryDialogFragment : BaseDialogFragment() {

    private lateinit var binding: NewLibraryDialogFragmentBinding

    private val viewModel by viewModel<NoteListViewModel> { parametersOf(args.libraryId) }

    private val args by navArgs<NewLibraryDialogFragmentArgs>()

    private val imm by lazy { requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NewLibraryDialogFragmentBinding.inflate(inflater, container, false)

        BaseDialogFragmentBinding.bind(binding.root).apply {
            if (args.libraryId == 0L) {
                tvDialogTitle.text = resources.stringResource(R.string.new_library)
            } else {
                tvDialogTitle.text = resources.stringResource(R.string.edit_library)
                binding.btnCreate.text = resources.stringResource(R.string.done)
            }
        }

        binding.et.requestFocus()
        imm.showKeyboard()
        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) { imm.hideKeyboard(binding.et.windowToken) }
            .isEnabled = true

        setupNotoColors()
        collectState()
        setupListeners()

        return binding.root
    }

    private fun collectState() {
        viewModel.library
            .onEach {
                binding.et.setText(it.title)
                binding.et.setSelection(it.title.length)
                binding.rgNotoColors.check(it.color.ordinal)
            }
            .launchIn(lifecycleScope)
    }

    private fun setupListeners() {
        binding.btnCreate.setOnClickListener {
            val title = binding.et.text.toString()
            val color = binding.rgNotoColors.checkedRadioButtonId.let {
                NotoColor.values().getOrElse(it) {
                    viewModel.library.value.color
                }
            }

            if (title.isBlank()) {
                binding.til.error = resources.stringResource(R.string.empty_title)
            } else {
                imm.hideKeyboard(binding.et.windowToken)
                dismiss()
                viewModel.createOrUpdateLibrary(title, color)
            }
        }

    }

    private fun setupNotoColors() {
        NotoColor.values().forEach { notoColor ->
            RadioButton(context).apply {
                id = notoColor.ordinal
                buttonDrawable = resources.drawableResource(R.drawable.selector_dialog_rbtn_gray)
                buttonTintList = resources.colorStateResource(notoColor.toResource())
                scaleX = 1.25F
                scaleY = 1.25F
                val layoutParams = RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT)
                layoutParams.setMargins(48, 48, 48, 48)
                this.layoutParams = layoutParams
                binding.rgNotoColors.addView(this)
            }
        }
        binding.rgNotoColors.setOnCheckedChangeListener { _, checkedId ->
            NotoColor.values()[checkedId].apply {
                binding.rgNotoColors.children
                    .map { it as RadioButton }
                    .onEach {
                        if (it.id == checkedId) {
                            it.buttonDrawable = resources.drawableResource(R.drawable.ic_sort_checked)
                            it.buttonTintList = resources.colorStateResource(toResource())
                        } else {
                            it.buttonDrawable = resources.drawableResource(R.drawable.selector_dialog_rbtn_gray)
                            it.buttonTintList = resources.colorStateResource(toResource())
                        }
                    }
                binding.til.setEndIconTintList(resources.colorStateResource(toResource()))
            }
        }
    }
}