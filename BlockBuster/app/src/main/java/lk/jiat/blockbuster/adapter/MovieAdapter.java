package lk.jiat.blockbuster.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.databinding.ItemListingBinding;
import lk.jiat.blockbuster.model.Movie;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private List<Movie> movies;
    private OnMovieClickListener listener;

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }

    public MovieAdapter(List<Movie> movies, OnMovieClickListener listener) {
        this.movies = movies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListingBinding binding = ItemListingBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
        params.width = (int) (parent.getResources().getDisplayMetrics().widthPixels * 0.48);
        binding.getRoot().setLayoutParams(params);

        return new MovieViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        return movies != null ? movies.size() : 0;
    }

    public void updateMovies(List<Movie> newMovies) {
        this.movies = newMovies;
        notifyDataSetChanged();
    }

    class MovieViewHolder extends RecyclerView.ViewHolder {

        private final ItemListingBinding binding;

        public MovieViewHolder(@NonNull ItemListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMovieClick(movies.get(position));
                }
            });
        }

        void bind(Movie movie) {
            binding.movieTitle.setText(movie.getTitle());
            binding.movieImdbRating.setText(String.valueOf(movie.getImdbRating()));
            binding.movieRtRating.setText(movie.getRtRating() + "%");

            if (movie.getImages() != null && !movie.getImages().isEmpty()) {
                String imagePath = movie.getImages().get(0);
                FirebaseStorage.getInstance()
                        .getReference()
                        .child(imagePath)
                        .getDownloadUrl()
                        .addOnSuccessListener(uri ->
                                Glide.with(binding.getRoot().getContext())
                                        .load(uri)
                                        .placeholder(R.drawable.movie_24px)
                                        .into(binding.movieImage)
                        )
                        .addOnFailureListener(e ->
                                binding.movieImage.setImageResource(R.drawable.movie_24px)
                        );
            } else {
                binding.movieImage.setImageResource(R.drawable.movie_24px);
            }
        }
    }
}