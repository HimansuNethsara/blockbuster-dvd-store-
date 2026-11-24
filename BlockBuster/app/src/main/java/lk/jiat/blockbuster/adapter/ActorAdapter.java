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
import lk.jiat.blockbuster.model.Actor;

public class ActorAdapter extends RecyclerView.Adapter<ActorAdapter.VH> {

    private List<Actor> list;

    public ActorAdapter(List<Actor> list) {
        this.list = list;
    }

    public static class VH extends RecyclerView.ViewHolder {

        TextView name;
        ImageView image;

        public VH(View v) {
            super(v);
            name = v.findViewById(R.id.castNameTextView);
            image = v.findViewById(R.id.castImageView);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cast, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

        Actor a = list.get(position);

        holder.name.setText(a.getName());

        FirebaseStorage.getInstance()
                .getReference()
                .child(a.getImagePath())
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
