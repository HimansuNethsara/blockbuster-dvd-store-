package lk.jiat.blockbuster.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.databinding.ItemListingBinding;
import lk.jiat.blockbuster.model.Movie;

public class RelatedMovieAdapter extends RecyclerView.Adapter<RelatedMovieAdapter.ViewHolder> {

    private List<Movie> movieList;

    public RelatedMovieAdapter(List<Movie> movieList) {
        this.movieList = movieList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ItemListingBinding binding = ItemListingBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        View itemView = binding.getRoot();

        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        if (params != null) {
            params.width = (int) (160 * parent.getResources().getDisplayMetrics().density);
            itemView.setLayoutParams(params);
        }

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Movie movie = movieList.get(position);

        holder.binding.movieTitle.setText(movie.getTitle());
        holder.binding.movieImdbRating.setText(String.valueOf(movie.getImdbRating()));
        holder.binding.movieRtRating.setText(movie.getRtRating() + "%");

        if (movie.getImages() != null && !movie.getImages().isEmpty()) {

            String imagePath = movie.getImages().get(0);

            FirebaseStorage.getInstance()
                    .getReference()
                    .child(imagePath)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri ->
                            Glide.with(holder.itemView.getContext())
                                    .load(uri)
                                    .placeholder(R.drawable.movie_24px)
                                    .into(holder.binding.movieImage)
                    )
                    .addOnFailureListener(e ->
                            holder.binding.movieImage.setImageResource(R.drawable.movie_24px)
                    );
        }
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ItemListingBinding binding;

        public ViewHolder(ItemListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}