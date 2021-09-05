package com.noto.app.library

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.noto.app.BaseDialogFragment
import com.noto.app.R
import com.noto.app.databinding.BaseDialogFragmentBinding
import com.noto.app.databinding.NewLibraryDialogFragmentBinding
import com.noto.app.domain.model.Library
import com.noto.app.domain.model.NotoColor
import com.noto.app.util.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NewLibraryDialogFragment : BaseDialogFragment() {

    private val viewModel by viewModel<LibraryViewModel> { parametersOf(args.libraryId) }

    private val args by navArgs<NewLibraryDialogFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        NewLibraryDialogFragmentBinding.inflate(inflater, container, false).withBinding {
            val baseDialogFragment = setupBaseDialogFragment()
            setupState(baseDialogFragment)
            setupListeners()
        }

    private fun NewLibraryDialogFragmentBinding.setupBaseDialogFragment() = BaseDialogFragmentBinding.bind(root).apply {
        if (args.libraryId == 0L) {
            tvDialogTitle.text = resources.stringResource(R.string.new_library)
        } else {
            tvDialogTitle.text = resources.stringResource(R.string.edit_library)
            btnCreate.text = resources.stringResource(R.string.done)
        }
    }

    private fun NewLibraryDialogFragmentBinding.setupState(baseDialogFragment: BaseDialogFragmentBinding) {
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.clipToOutline = true
        if (args.libraryId == 0L) {
            et.requestFocus()
            requireActivity().showKeyboard(root)
        }

        viewModel.state
            .onEach { state -> setupLibrary(state.library, baseDialogFragment) }
            .launchIn(lifecycleScope)

        viewModel.notoColors
            .onEach { pairs -> setupNotoColors(pairs) }
            .launchIn(lifecycleScope)
    }

    private fun NewLibraryDialogFragmentBinding.setupListeners() {
        btnCreate.setOnClickListener {
            val title = et.text.toString()
            if (title.isBlank()) {
                til.isErrorEnabled = true
                til.error = resources.stringResource(R.string.empty_title)
            } else {
                requireActivity().hideKeyboard(root)
                dismiss()
                updatePinnedShortcut(title)
                viewModel.createOrUpdateLibrary(title, sNotePreviewSize.value.toInt(), swShowNoteCreationDate.isChecked)
            }
        }
    }

    private fun NewLibraryDialogFragmentBinding.setupLibrary(library: Library, baseDialogFragment: BaseDialogFragmentBinding) {
        et.setText(library.title)
        et.setSelection(library.title.length)
        rv.smoothScrollToPosition(library.color.ordinal)
        val state = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked),
        )
        swShowNoteCreationDate.isChecked = library.isShowNoteCreationDate
        val switchOffColor = ColorUtils.setAlphaComponent(resources.colorResource(R.color.colorOnSecondary), 128)
        if (library.id != 0L) {
            val color = resources.colorResource(library.color.toResource())
            val colorStateList = resources.colorStateResource(library.color.toResource())
            baseDialogFragment.tvDialogTitle.setTextColor(color)
            baseDialogFragment.vHead.background?.mutate()?.setTint(color)
            if (colorStateList != null) {
                sNotePreviewSize.value = library.notePreviewSize.toFloat()
                sNotePreviewSize.trackActiveTintList = colorStateList
                sNotePreviewSize.thumbTintList = colorStateList
                sNotePreviewSize.tickInactiveTintList = colorStateList
                swShowNoteCreationDate.thumbTintList = ColorStateList(
                    state,
                    intArrayOf(
                        color,
                        resources.colorResource(R.color.colorSurface)
                    )
                )
                swShowNoteCreationDate.trackTintList = ColorStateList(
                    state,
                    intArrayOf(
                        ColorUtils.setAlphaComponent(color, 128),
                        switchOffColor,
                    )
                )
            }
        } else {
            swShowNoteCreationDate.thumbTintList = ColorStateList(
                state,
                intArrayOf(
                    resources.colorResource(R.color.colorPrimary),
                    resources.colorResource(R.color.colorSurface),
                )
            )
            swShowNoteCreationDate.trackTintList = ColorStateList(
                state,
                intArrayOf(
                    ColorUtils.setAlphaComponent(resources.colorResource(R.color.colorPrimary), 128),
                    switchOffColor,
                )
            )
        }
    }

    private fun NewLibraryDialogFragmentBinding.setupNotoColors(pairs: List<Pair<NotoColor, Boolean>>) {
        rv.withModels {
            pairs.forEach { pair ->
                notoColorItem {
                    id(pair.first.ordinal)
                    notoColor(pair.first)
                    isChecked(pair.second)
                    onClickListener { _ ->
                        viewModel.selectNotoColor(pair.first)
                    }
                }
            }
        }
    }

    private fun NewLibraryDialogFragmentBinding.updatePinnedShortcut(title: String) {
        val library = viewModel.state.value.library.copy(
            title = title,
            color = viewModel.notoColors.value.first { it.second }.first
        )
        ShortcutManagerCompat.updateShortcuts(requireContext(), listOf(requireContext().createPinnedShortcut(library)))
    }
}