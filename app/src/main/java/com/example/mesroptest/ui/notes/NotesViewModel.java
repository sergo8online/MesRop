package com.example.mesroptest.ui.notes;
import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.mesroptest.data.AppDatabase;
import com.example.mesroptest.data.Note;
import com.example.mesroptest.data.NoteDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotesViewModel extends AndroidViewModel {
    private final NoteDao noteDao;
    private final LiveData<List<Note>> allNotes;
    private final ExecutorService executorService;

    public NotesViewModel(Application application) {
        super(application);
        noteDao = AppDatabase.getInstance(application).noteDao();
        allNotes = noteDao.getAllNotes();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }

    public void insert(Note note) {
        executorService.execute(() -> noteDao.insert(note));
    }

    public void delete(Note note) {
        executorService.execute(() -> noteDao.delete(note));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }

    public void update(Note note) {
        executorService.execute(() -> noteDao.update(note));
    }

}
