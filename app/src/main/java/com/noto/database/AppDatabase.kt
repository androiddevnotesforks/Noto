package com.noto.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.noto.note.model.Note
import com.noto.note.model.Notebook
import com.noto.todo.model.Todo
import com.noto.todo.model.TodoList

@TypeConverters(NotoColorConverter::class)
@Database(
    entities = [Notebook::class, Note::class, TodoList::class, Todo::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract val notebookDao: NotebookDao
    abstract val noteDao: NoteDao

    abstract val todoListDao: TodoListDao
    abstract val todoDao: TodoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        "AppDatabase"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}