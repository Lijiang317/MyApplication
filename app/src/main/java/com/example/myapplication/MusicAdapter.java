package com.example.myapplication; // 确保这里的包名和你 MainActivity 的第一行一样

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private List<String> songNames;
    private List<String> songPaths;
    private OnItemClickListener listener;

    // 定义点击接口，方便在 Activity 里处理点击播放
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public MusicAdapter(List<String> names, List<String> paths, OnItemClickListener listener) {
        this.songNames = names;
        this.songPaths = paths;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        holder.tvTitle.setText(songNames.get(position));
        holder.tvPath.setText(songPaths.get(position));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return songNames.size();
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPath;
        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.item_music_title);
            tvPath = itemView.findViewById(R.id.item_music_path);
        }
    }
}