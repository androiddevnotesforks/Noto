package com.noto.app.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.domain.model.*
import com.noto.app.domain.repository.*
import com.noto.app.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

private val ExtraDatePeriod = DatePeriod(days = 1)

class NoteViewModel(
    private val folderRepository: FolderRepository,
    private val noteRepository: NoteRepository,
    private val labelRepository: LabelRepository,
    private val noteLabelRepository: NoteLabelRepository,
    private val settingsRepository: SettingsRepository,
    private val folderId: Long,
    private val noteId: Long,
    private val body: String?,
    private var labelsIds: LongArray,
) : ViewModel() {

    private val mutableNote = MutableStateFlow(Note(noteId, folderId, position = 0))
    val note get() = mutableNote.asStateFlow()

    private val mutableTitleHistory = MutableSharedFlow<Triple<Int, Int, String>>(replay = Int.MAX_VALUE)
    val titleHistory get() = mutableTitleHistory.asSharedFlow()

    private val mutableBodyHistory = MutableSharedFlow<Triple<Int, Int, String>>(replay = Int.MAX_VALUE)
    val bodyHistory get() = mutableBodyHistory.asSharedFlow()

    private val mutableIsUndoOrRedo = MutableStateFlow(false)
    val isUndoOrRedo get() = mutableIsUndoOrRedo.asStateFlow()

    val folder = folderRepository.getFolderById(folderId)
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Lazily, Folder(position = 0))

    val font = settingsRepository.font
        .stateIn(viewModelScope, SharingStarted.Lazily, Font.Nunito)

    private val mutableLabels = MutableStateFlow<Map<Label, Boolean>>(emptyMap())
    val labels get() = mutableLabels.asStateFlow()

    val isRememberScrollingPosition = settingsRepository.isRememberScrollingPosition
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val mutableIsTrackingTitleCursorPosition = MutableStateFlow(false)
    val isTrackingTitleCursorPosition get() = mutableIsTrackingTitleCursorPosition.asStateFlow()

    private val mutableIsTrackingBodyCursorPosition = MutableStateFlow(false)
    val isTrackingBodyCursorPosition get() = mutableIsTrackingBodyCursorPosition.asStateFlow()

    private var titleCursorStartPosition = 0
    private var titleCursorEndPosition = 0

    private var bodyCursorStartPosition = 0
    private var bodyCursorEndPosition = 0

    private val mutableReminderDate = MutableStateFlow(Clock.System.now().toLocalDate().plus(ExtraDatePeriod))
    val reminderDate get() = mutableReminderDate.asStateFlow()

    private val mutableReminderTime = MutableStateFlow(Clock.System.now().toLocalTime())
    val reminderTime get() = mutableReminderTime.asStateFlow()

    init {
        noteRepository.getNoteById(noteId)
            .onStart { emit(Note(noteId, folderId, position = 0, title = body.firstLineOrEmpty(), body = body.takeAfterFirstLineOrEmpty())) }
            .filterNotNull()
            .onEach {
                mutableNote.value = it
                if (it.reminderDate != null) {
                    val dateTime = it.reminderDate.toLocalDateTime(TimeZone.currentSystemDefault())
                    mutableReminderDate.value = dateTime.date
                    mutableReminderTime.value = dateTime.time
                }
            }
            .launchIn(viewModelScope)

        var selectedLabels: List<Pair<Label, Boolean>>? = null

        combine(
            labelRepository.getLabelsByFolderId(folderId)
                .filterNotNull(),
            noteLabelRepository.getNoteLabels()
                .filterNotNull(),
        ) { labels, noteLabels ->
            labels
                .map { label ->
                    val isSelected = noteLabels.filter { it.noteId == note.value.id }
                        .any { it.labelId == label.id } || labelsIds.any { it == label.id }
                    label to isSelected
                }
                .let { selectableLabels ->
                    val isReordered = selectedLabels.orEmpty().any { selectedLabel ->
                        val label = selectableLabels.first { it.first.id == selectedLabel.first.id }
                        selectedLabel.first.position != label.first.position
                    }
                    selectedLabels = when {
                        selectedLabels == null || isReordered -> selectableLabels.filter { it.second }
                        else -> selectedLabels.orEmpty().map { label ->
                            selectableLabels.first { it.first.id == label.first.id }
                        }
                    }.sortedBy { it.first.position }
                    selectedLabels.orEmpty() + selectableLabels
                        .filterNot { label -> selectedLabels.orEmpty().any { it.first.id == label.first.id } }
                        .sortedBy { it.first.position }
                }
                .toMap()
        }
            .onEach {
                mutableLabels.value = it
                labelsIds = longArrayOf() // Setting this value to empty array so it can be used only once.
            }
            .launchIn(viewModelScope)
    }

    fun createOrUpdateNote(title: String, body: String, trimContent: Boolean) = viewModelScope.launch {
        val note = note.value.copy(
            title = title.takeUnless { trimContent } ?: title.trim(),
            body = body.takeUnless { trimContent } ?: body.trim(),
        )
        if (note.isValid) {
            if (note.id == 0L) {
                val newNoteId = noteRepository.createNote(note)
                noteRepository.getNoteById(newNoteId)
                    .filterNotNull()
                    .onEach { createdNote -> mutableNote.value = createdNote }
                    .launchIn(viewModelScope)

                labels.value.filterValues { it }.keys
                    .map { label -> NoteLabel(noteId = newNoteId, labelId = label.id) }
                    .forEach { noteLabel -> noteLabelRepository.createNoteLabel(noteLabel) }
            } else {
                noteRepository.updateNote(note)
            }
        }
    }

    fun deleteNote() = viewModelScope.launch {
        noteRepository.deleteNote(note.value)
    }

    fun toggleNoteIsArchived() = viewModelScope.launch {
        noteRepository.updateNote(note.value.copy(isArchived = !note.value.isArchived, reminderDate = null))
    }

    fun toggleNoteIsPinned() = viewModelScope.launch {
        noteRepository.updateNote(note.value.copy(isPinned = !note.value.isPinned))
    }

    fun setNoteReminder(): Instant {
        val dateTime = LocalDateTime(mutableReminderDate.value, mutableReminderTime.value)
        val instant = dateTime.toInstant(TimeZone.currentSystemDefault())
        viewModelScope.launch {
            noteRepository.updateNote(note.value.copy(reminderDate = instant))
        }
        return instant
    }

    fun cancelNoteReminder() = viewModelScope.launch {
        noteRepository.updateNote(note.value.copy(reminderDate = null))
    }

    fun moveNote(folderId: Long) = viewModelScope.launch {
        noteRepository.updateNote(note.value.copy(folderId = folderId))
        labels.value.filterSelected().forEach { label ->
            launch {
                val labelId = labelRepository.getOrCreateLabel(folderId, label)
                noteLabelRepository.createNoteLabel(NoteLabel(labelId = labelId, noteId = note.value.id))
                noteLabelRepository.deleteNoteLabel(note.value.id, label.id)
            }
        }
    }

    fun copyNote(folderId: Long) = viewModelScope.launch {
        val noteId = noteRepository.createNote(note.value.copy(id = 0, folderId = folderId))
        labels.value.filterSelected().forEach { label ->
            launch {
                val labelId = labelRepository.getOrCreateLabel(folderId, label)
                noteLabelRepository.createNoteLabel(NoteLabel(labelId = labelId, noteId = noteId))
            }
        }
    }

    fun duplicateNote() = viewModelScope.launch {
        val noteId = noteRepository.createNote(note.value.copy(id = 0, reminderDate = null))
        labels.value.filterSelected().forEach { label ->
            launch {
                noteLabelRepository.createNoteLabel(NoteLabel(noteId = noteId, labelId = label.id))
            }
        }
    }

    fun selectLabel(id: Long) = viewModelScope.launch {
        if (note.value.id == 0L)
            mutableLabels.value = labels.value.mapValues { if (it.key.id == id) true else it.value }
        else
            noteLabelRepository.createNoteLabel(NoteLabel(noteId = note.value.id, labelId = id))

    }

    fun unselectLabel(id: Long) = viewModelScope.launch {
        if (note.value.id == 0L)
            mutableLabels.value = labels.value.mapValues { if (it.key.id == id) false else it.value }
        else
            noteLabelRepository.deleteNoteLabel(note.value.id, id)
    }

    fun updateNoteScrollingPosition(scrollingPosition: Int) = viewModelScope.launch {
        noteRepository.updateNote(note.value.copy(scrollingPosition = scrollingPosition))
    }

    fun updateNoteAccessDate() = viewModelScope.launch {
        noteRepository.getNoteById(noteId)
            .firstOrNull()
            ?.let { note -> noteRepository.updateNote(note.copy(accessDate = Clock.System.now())) }
    }

    fun emitNewTitleOnly(title: String) = viewModelScope.launch {
        val value = Triple(titleCursorStartPosition, titleCursorEndPosition, title)
        val isNew = bodyHistory.replayCache.none { it.third == title }
        if (isNew) mutableTitleHistory.emit(value)
        setIsTrackingTitleCursorPosition(false)
    }

    fun emitNewBodyOnly(body: String) = viewModelScope.launch {
        val value = Triple(bodyCursorStartPosition, bodyCursorEndPosition, body)
        val isNew = bodyHistory.replayCache.none { it.third == body }
        if (isNew) mutableBodyHistory.emit(value)
        setIsTrackingBodyCursorPosition(false)
    }

    fun undoTitle(): Triple<Int, Int, String> {
        val value = titleHistory.replayCache.getPreviousValueOrCurrent(note.value.title)
        setIsUndoOrRedo()
        setNoteTitle(value.third)
        return value
    }

    fun redoTitle(): Triple<Int, Int, String> {
        val value = titleHistory.replayCache.getNextValueOrCurrent(note.value.title)
        setIsUndoOrRedo()
        setNoteTitle(value.third)
        return value
    }

    fun undoBody(): Triple<Int, Int, String> {
        val value = bodyHistory.replayCache.getPreviousValueOrCurrent(note.value.body)
        setIsUndoOrRedo()
        setNoteBody(value.third)
        return value
    }

    fun redoBody(): Triple<Int, Int, String> {
        val value = bodyHistory.replayCache.getNextValueOrCurrent(note.value.body)
        setIsUndoOrRedo()
        setNoteBody(value.third)
        return value
    }

    fun setIsUndoOrRedo() {
        mutableIsUndoOrRedo.value = true
    }

    fun resetIsUndoOrRedo() {
        mutableIsUndoOrRedo.value = false
    }

    fun setNoteTitle(title: String) {
        mutableNote.value = note.value.copy(title = title)
    }

    fun setNoteBody(body: String) {
        mutableNote.value = note.value.copy(body = body)
    }

    fun setIsTrackingTitleCursorPosition(value: Boolean) {
        mutableIsTrackingTitleCursorPosition.value = value
    }

    fun setIsTrackingBodyCursorPosition(value: Boolean) {
        mutableIsTrackingBodyCursorPosition.value = value
    }

    fun setTitleCursorStartPosition(position: Int) {
        titleCursorStartPosition = position
    }

    fun setTitleCursorEndPosition(position: Int) {
        titleCursorEndPosition = position
    }

    fun setBodyCursorStartPosition(position: Int) {
        bodyCursorStartPosition = position
    }

    fun setBodyCursorEndPosition(position: Int) {
        bodyCursorEndPosition = position
    }

    fun setReminderDate(date: LocalDate) {
        mutableReminderDate.value = date
    }

    fun setReminderTime(time: LocalTime) {
        mutableReminderTime.value = time
    }

    private fun List<Triple<Int, Int, String>>.getPreviousValueOrCurrent(currentValue: String): Triple<Int, Int, String> {
        val lastIndex = lastIndex.coerceAtLeast(0)
        return indexOfLast { it.third == currentValue }.minus(1).coerceIn(0, lastIndex).let(this::get)
    }

    private fun List<Triple<Int, Int, String>>.getNextValueOrCurrent(currentValue: String): Triple<Int, Int, String> {
        val lastIndex = lastIndex.coerceAtLeast(0)
        return indexOfFirst { it.third == currentValue }.plus(1).coerceIn(0, lastIndex).let(this::get)
    }
}