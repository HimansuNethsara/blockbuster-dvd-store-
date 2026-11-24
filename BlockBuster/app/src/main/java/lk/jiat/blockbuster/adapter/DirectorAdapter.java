package lk.jiat.blockbuster.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.model.Director;

public class DirectorAdapter extends RecyclerView.Adapter<DirectorAdapter.VH> {

    private List<Director> list;

    public DirectorAdapter(List<Director> list) {
        this.list = list;
    }

    public static class VH extends RecyclerView.ViewHolder {

        TextView name;
        ImageView image;

        public VH(View v) {
            super(v);
            name = v.findViewById(R.id.directorNameTextView);
            image = v.findViewById(R.id.directorImageView);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_director, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

        Director d = list.get(position);

        holder.name.setText(d.getName());

        FirebaseStorage.getInstance()
                .getReference()
                .child(d.getImagePath())
                .getDownloadUrl()
                .addOnSuccessListener(uri ->
                        Glide.with(holder.image.getContext())
                                .load(uri)
                                .into(holder.image)
                );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
