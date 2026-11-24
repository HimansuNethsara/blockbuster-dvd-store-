package lk.jiat.blockbuster.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.model.Genre;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.ViewHolder> {

    private List<Genre> genres;
    private OnGenreClickListener listener;
    private FirebaseStorage storage;

    public GenreAdapter(List<Genre> genres, OnGenreClickListener listener){
        this.genres = genres;
        this.listener = listener;
        storage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public GenreAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_genre, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreAdapter.ViewHolder holder, int position) {
        Genre genre = genres.get(position);
        holder.genreName.setText(genre.getName());

        Glide.with(holder.itemView.getContext()).clear(holder.genreImage);
        holder.genreImage.setImageDrawable(null);

        if (genre.getDownloadUri() != null) {
            // Use cached URL
            Glide.with(holder.itemView.getContext())
                    .load(genre.getDownloadUri())
                    .placeholder(R.drawable.movie_24px)
                    .error(R.drawable.movie_24px)
                    .centerCrop()
                    .into(holder.genreImage);
        } else {
            // Fetch download URL and cache it
            storage.getReference(genre.getImageUrl())
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        int currentPosition = holder.getBindingAdapterPosition();
                        if (currentPosition == RecyclerView.NO_POSITION) return;

                        Genre currentGenre = genres.get(currentPosition);
                        if (!currentGenre.getImageUrl().equals(genre.getImageUrl())) return;

                        genre.setDownloadUri(uri.toString());

                        Glide.with(holder.itemView.getContext())
                                .load(uri)
                                .placeholder(R.drawable.movie_24px)
                                .error(R.drawable.movie_24px)
                                .centerCrop()
                                .into(holder.genreImage);
                    })
                    .addOnFailureListener(e -> Glide.with(holder.itemView.getContext())
                            .load(R.drawable.movie_24px)
                            .centerCrop()
                            .into(holder.genreImage));
        }

        holder.itemView.setOnClickListener(v -> {

            Animation animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.click_animation);
            v.startAnimation(animation);

            if (listener != null) {
                listener.onGenreClick(genre);
            }
        });
    }

    @Override
    public int getItemCount() {
        return genres.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView genreImage;
        TextView genreName;
        public ViewHolder(@NonNull View itemView){
            super(itemView);
            genreImage = itemView.findViewById(R.id.genre_image);
            genreName = itemView.findViewById(R.id.genre_name);
        }
    }

    public interface OnGenreClickListener {
        void onGenreClick(Genre genre);
    }


}

