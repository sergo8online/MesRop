package com.example.mesroptest.ui.notes;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mesroptest.data.Note;
import com.example.mesroptest.databinding.ItemNoteBinding;

public class NotesAdapter extends ListAdapter<Note, NotesAdapter.NoteViewHolder> {
    private final OnNoteClickListener listener;

    public NotesAdapter(OnNoteClickListener listener) {
        super(new NoteDiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoteBinding binding;

        NoteViewHolder(ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Исправленный onClickListener
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onNoteClick(getItem(position));
                }
            });

            // Исправленный onLongClickListener
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onNoteLongClick(getItem(position));
                    return true;
                }
                return false;
            });
        }

        void bind(Note note) {
            binding.titleTextView.setText(note.getTitle());
            binding.contentTextView.setText(note.getContent());
        }
    }

    private static class NoteDiffCallback extends DiffUtil.ItemCallback<Note> {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getContent().equals(newItem.getContent());
        }
    }

    // ✅ Добавляем недостающий метод в интерфейс
    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteLongClick(Note note);
    }
}