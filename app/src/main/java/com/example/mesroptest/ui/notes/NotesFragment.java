package com.example.mesroptest.ui.notes;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mesroptest.data.Note;
import com.example.mesroptest.databinding.FragmentNotesBinding;

public class NotesFragment extends Fragment {
    private FragmentNotesBinding binding;
    private NotesViewModel viewModel;
    private NotesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotesBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);

        setupRecyclerView();
        setupAddButton();
        observeNotes();

        return binding.getRoot();
    }

    public void saveEditedNote(Note note) {
        // Сохраняем изменения в базе данных
        viewModel.update(note);

        // После сохранения обновляем UI
        viewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
        });
    }



    private void setupRecyclerView() {
            adapter = new NotesAdapter(new NotesAdapter.OnNoteClickListener() {
                @Override
                public void onNoteClick(Note note) {
                    showEditDialog(note); // Открываем заметку для редактирования
                }

                @Override
                public void onNoteLongClick(Note note) {
                    showDeleteDialog(note); // Долгое нажатие — удаление заметки
                }
            });

            binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.recyclerView.setAdapter(adapter);
        }

//        adapter = new NotesAdapter(new NotesAdapter.OnNoteClickListener() {
//            @Override
//            public void onNoteClick(Note note) {
//                showEditDialog(note); // Открывает заметку для редактирования
//            }
//
//            @Override
//            public void onNoteLongClick(Note note) {
//                showDeleteDialog(note); // Долгое нажатие удаляет заметку
//            }
//        });
//
//        adapter = new NotesAdapter(note -> showDeleteDialog(note));
//        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
//        binding.recyclerView.setAdapter(adapter);

    private void setupAddButton() {
        binding.addButton.setOnClickListener(v -> showAddNoteDialog());
    }

    private void observeNotes() {
        viewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
            binding.emptyView.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showAddNoteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Note");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        EditText titleInput = new EditText(requireContext());
        titleInput.setHint("Title");
        layout.addView(titleInput);

        EditText contentInput = new EditText(requireContext());
        contentInput.setHint("Content");
        layout.addView(contentInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String content = contentInput.getText().toString().trim();
            if (!title.isEmpty() && !content.isEmpty()) {
                viewModel.insert(new Note(title, content));
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteDialog(Note note) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.delete(note))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Note");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        EditText titleInput = new EditText(requireContext());
        titleInput.setText(note.getTitle());
        layout.addView(titleInput);

        EditText contentInput = new EditText(requireContext());
        contentInput.setText(note.getContent());
        layout.addView(contentInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTitle = titleInput.getText().toString().trim();
            String newContent = contentInput.getText().toString().trim();
            if (!newTitle.isEmpty() && !newContent.isEmpty()) {
                note.setTitle(newTitle);
                note.setContent(newContent);
                viewModel.update(note); // Перезаписываем заметку
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
