package com.noto.app.librarylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.noto.app.BaseDialogFragment
import com.noto.app.R
import com.noto.app.databinding.LibraryListFragmentBinding
import com.noto.app.domain.model.Library
import com.noto.app.util.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

class LibraryListFragment : BaseDialogFragment() {

    private lateinit var binding: LibraryListFragmentBinding

    private val viewModel by viewModel<LibraryListViewModel>()

    private val args by navArgs<LibraryListFragmentArgs>()

    private val adapter by lazy { LibraryListAdapter(libraryItemClickListener) }

    private val libraryItemClickListener by lazy {
        object : LibraryListAdapter.LibraryItemClickListener {
            override fun onClick(library: Library) {
                if (args.content == null)
                    findNavController().navigate(LibraryListFragmentDirections.actionLibraryListFragmentToLibraryFragment(library.id))
                else
                    findNavController().navigate(LibraryListFragmentDirections.actionLibraryListFragmentToNoteFragment(library.id, body = args.content))
            }

            override fun onLongClick(library: Library) = findNavController().navigate(LibraryListFragmentDirections.actionLibraryListFragmentToLibraryDialogFragment(library.id))
            override fun countLibraryNotes(library: Library): Int = viewModel.countNotes(library.id)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LibraryListFragmentBinding.inflate(inflater, container, false)

        setupListeners()
        collectState()
        binding.rv.adapter = adapter

        return binding.root
    }

    private fun setupListeners() {
        binding.fab.setOnClickListener {
            findNavController().navigate(LibraryListFragmentDirections.actionLibraryListFragmentToNewLibraryDialogFragment())
        }

        binding.bab.setNavigationOnClickListener {
            findNavController().navigate(LibraryListFragmentDirections.actionLibraryListFragmentToLibraryListDialogFragment())
        }

        binding.bab.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.layout_manager -> {
                    when (viewModel.layoutManager.value) {
                        LayoutManager.Linear -> {
                            viewModel.updateLayoutManager(LayoutManager.Grid)
                            binding.root.snackbar(getString(R.string.layout_is_grid_mode), anchorView = binding.fab)
                        }
                        LayoutManager.Grid -> {
                            viewModel.updateLayoutManager(LayoutManager.Linear)
                            binding.root.snackbar(getString(R.string.layout_is_list_mode), anchorView = binding.fab)
                        }
                    }
                    true
                }
                R.id.theme -> {
                    findNavController().navigate(LibraryListFragmentDirections.actionLibraryListFragmentToThemeDialogFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun collectState() {
        val layoutManagerMenuItem = binding.bab.menu.findItem(R.id.layout_manager)

        viewModel.layoutManager
            .onEach {
                when (it) {
                    LayoutManager.Linear -> {
                        layoutManagerMenuItem.icon = resources.drawableResource(R.drawable.ic_round_view_grid_24)
                        binding.rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    }
                    LayoutManager.Grid -> {
                        layoutManagerMenuItem.icon = resources.drawableResource(R.drawable.ic_round_view_agenda_24)
                        binding.rv.layoutManager = GridLayoutManager(requireContext(), 2)
                    }
                }
                binding.rv.visibility = View.INVISIBLE
                binding.rv.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.hide))

                binding.rv.visibility = View.VISIBLE
                binding.rv.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.show))
            }
            .launchIn(lifecycleScope)

        val layoutItems = listOf(binding.tvLibrariesCount, binding.rv)

        viewModel.libraries
            .onEach {
                if (it.isEmpty()) {
                    layoutItems.forEach { it.visibility = View.GONE }
                    binding.tvPlaceHolder.visibility = View.VISIBLE
                } else {
                    layoutItems.forEach { it.visibility = View.VISIBLE }
                    binding.tvPlaceHolder.visibility = View.GONE
                    adapter.submitList(it)
                    binding.tvLibrariesCount.text = it.size.toCountText(resources.stringResource(R.string.library), resources.stringResource(R.string.libraries))
                }
            }
            .launchIn(lifecycleScope)

    }

}