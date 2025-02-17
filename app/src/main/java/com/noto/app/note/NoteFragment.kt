package com.noto.app.note

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.noto.app.R
import com.noto.app.databinding.NoteFragmentBinding
import com.noto.app.util.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class NoteFragment : Fragment() {

    private val viewModel by viewModel<NoteViewModel> { parametersOf(args.libraryId, args.noteId, args.body) }

    private val args by navArgs<NoteFragmentArgs>()

    private val imm by lazy { requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = NoteFragmentBinding.inflate(inflater, container, false).withBinding {

        fab.setOnClickListener {
            findNavController()
                .navigate(NoteFragmentDirections.actionNotoFragmentToReminderDialogFragment(args.libraryId, args.noteId))
        }

        if (args.noteId == 0L) {
            etNoteBody.requestFocus()
            imm.showKeyboard()
        }

        val backCallback = {
            if (args.body != null)
                findNavController().popBackStack(R.id.libraryListFragment, false)
            findNavController().navigateUp()
            viewModel.createOrUpdateNote(
                etNoteTitle.text.toString(),
                etNoteBody.text.toString(),
            )
            imm.hideKeyboard(etNoteBody.windowToken)
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) { backCallback() }
            .isEnabled = true

        tb.setNavigationOnClickListener {
            backCallback()
        }

        nsv.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.show))

        with(bab) {

            setNavigationOnClickListener {
                findNavController().navigate(NoteFragmentDirections.actionNotoFragmentToNotoDialogFragment(args.libraryId, args.noteId, R.id.libraryFragment))
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.share_noto -> {
                        launchShareNoteIntent(viewModel.note.value)
                        true
                    }
                    R.id.archive_note -> {
                        if (viewModel.note.value.isArchived) {
                            viewModel.toggleNoteIsArchived()
                            root.snackbar(getString(R.string.note_unarchived), anchorView = fab)
                        } else {
                            viewModel.toggleNoteIsArchived()
                            root.snackbar(getString(R.string.note_archived), anchorView = fab)
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        val archiveMenuItem = bab.menu.findItem(R.id.archive_note)

        viewModel.note
            .filterNotNull()
            .onEach {
                etNoteTitle.setText(it.title)
                etNoteBody.setText(it.body)
                etNoteTitle.setSelection(it.title.length)
                etNoteBody.setSelection(it.body.length)
                tvCreatedAt.text = "${getString(R.string.created)} ${it.formatCreationDate()}"

                if (it.isArchived) archiveMenuItem.icon = resources.drawableResource(R.drawable.ic_round_unarchive_24)
                else archiveMenuItem.icon = resources.drawableResource(R.drawable.ic_round_archive_24)

                if (it.reminderDate == null) fab.setImageDrawable(resources.drawableResource(R.drawable.ic_round_notification_add_24))
                else fab.setImageDrawable(resources.drawableResource(R.drawable.ic_round_edit_notifications_24))
            }
            .launchIn(lifecycleScope)

        viewModel.library
            .onEach {
                val color = resources.colorResource(it.color.toResource())

                tb.title = it.title
                tb.setTitleTextColor(color)
                tvCreatedAt.setTextColor(color)
                tb.navigationIcon?.mutate()?.setTint(color)
                fab.backgroundTintList = resources.colorStateResource(it.color.toResource())
            }
            .launchIn(lifecycleScope)
    }
}