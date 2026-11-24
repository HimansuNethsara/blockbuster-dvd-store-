package lk.jiat.blockbuster.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.databinding.ItemListingBinding;
import lk.jiat.blockbuster.model.Movie;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ViewHolder> {

    private List<Movie> movies;
    private OnListingItemClickListener listener;
    private FirebaseStorage storage;

    public ListingAdapter(List<Movie> movies, OnListingItemClickListener listener) {
        this.movies = movies;
        this.listener = listener;
        storage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public ListingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListingBinding binding = ItemListingBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingAdapter.ViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.binding.movieTitle.setText(movie.getTitle());
        holder.binding.movieImdbRating.setText(String.valueOf(movie.getImdbRating()));
        holder.binding.movieRtRating.setText(String.valueOf(movie.getRtRating()));

        Glide.with(holder.itemView.getContext()).clear(holder.binding.movieImage);
        holder.binding.movieImage.setImageDrawable(null);

        if (movie.getDownloadUri() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(movie.getDownloadUri())
                    .placeholder(R.drawable.movie_24px)
                    .error(R.drawable.movie_24px)
                    .centerCrop()
                    .into(holder.binding.movieImage);
        } else {

            String imagePath = "movies/" + movie.getMovieId() + "/poster.jpg";

            storage.getReference(imagePath)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        int currentPosition = holder.getBindingAdapterPosition();
                        if (currentPosition == RecyclerView.NO_POSITION) return;

                        Movie currentMovie = movies.get(currentPosition);
                        if (!currentMovie.getMovieId().equals(movie.getMovieId())) return;

                        movie.setDownloadUri(uri.toString());

                        Glide.with(holder.itemView.getContext())
                                .load(uri)
                                .placeholder(R.drawable.movie_24px)
                                .error(R.drawable.movie_24px)
                                .centerCrop()
                                .into(holder.binding.movieImage);
                    })
                    .addOnFailureListener(e -> Glide.with(holder.itemView.getContext())
                            .load(R.drawable.movie_24px)
                            .centerCrop()
                            .into(holder.binding.movieImage));
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            Animation animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.click_animation);
            v.startAnimation(animation);

            if (listener != null){
                listener.onListingItemClick(movie);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemListingBinding binding;

        public ViewHolder(ItemListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnListingItemClickListener{
        void onListingItemClick(Movie movie);
    }
}

