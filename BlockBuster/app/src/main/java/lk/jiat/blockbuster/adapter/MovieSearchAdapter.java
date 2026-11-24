package lk.jiat.blockbuster.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.databinding.ItemSearchSuggestionBinding;
import lk.jiat.blockbuster.model.Dvd;
import lk.jiat.blockbuster.model.Movie;

public class MovieSearchAdapter extends ArrayAdapter<Movie> {

    private Context context;
    private List<Movie> originalList;
    private List<Movie> filteredList;

    public MovieSearchAdapter(@NonNull Context context, @NonNull List<Movie> movies) {
        super(context, 0, movies);
        this.context = context;
        this.originalList = new ArrayList<>(movies);
        this.filteredList = new ArrayList<>(movies);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ItemSearchSuggestionBinding binding;

        if (convertView == null) {
            binding = ItemSearchSuggestionBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
            );
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (ItemSearchSuggestionBinding) convertView.getTag();
        }

        Movie movie = getItem(position);

        if (movie == null) {
            return convertView;
        }

        String movieId = movie.getMovieId();

        binding.suggestionProductName.setTag(movieId);
        binding.suggestionProductName.setText(movie.getTitle());
        binding.suggestionProductImage.setImageResource(R.drawable.movie_24px);

        if (movie.getImages() != null && !movie.getImages().isEmpty()) {

            FirebaseStorage.getInstance()
                    .getReference()
                    .child(movie.getImages().get(0))
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {

                        Object tag = binding.suggestionProductName.getTag();

                        if (tag != null && tag.equals(movieId)) {

                            Glide.with(context)
                                    .load(uri)
                                    .placeholder(R.drawable.movie_24px)
                                    .error(R.drawable.movie_24px)
                                    .centerCrop()
                                    .into(binding.suggestionProductImage);
                        }
                    });
        }

        binding.suggestionProductPrice.setText("Loading...");
        loadPrice(binding, movieId);

        if (showQuickAction) {
            binding.suggestionQuickAction.setVisibility(View.VISIBLE);

            binding.suggestionQuickAction.setOnClickListener(v -> {
                if (quickActionListener != null) {
                    quickActionListener.onQuickActionClick(movie);
                }
            });

        } else {
            binding.suggestionQuickAction.setVisibility(View.GONE);
        }

        return convertView;
    }

    private void loadPrice(ItemSearchSuggestionBinding binding, String movieId) {

        FirebaseFirestore.getInstance()
                .collection("dvds")
                .whereEqualTo("movieId", movieId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.isEmpty()) {

                        Dvd dvd = snapshot.getDocuments().get(0).toObject(Dvd.class);

                        if (dvd != null) {

                            Object tag = binding.suggestionProductName.getTag();

                            if (tag != null && tag.equals(movieId)) {

                                binding.suggestionProductPrice.setText(
                                        String.format("Rs %,.2f", dvd.getPrice())
                                );
                            }
                        }
                    }
                });
    }

    @NonNull
    @Override
    public android.widget.Filter getFilter() {
        return new android.widget.Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                List<Movie> list = new ArrayList<>();

                if (constraint == null || constraint.length() < 2) {
                    list.clear();
                } else {
                    String query = constraint.toString().toLowerCase();

                    for (Movie m : originalList) {
                        if (m.getTitle().toLowerCase().contains(query)) {
                            list.add(m);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = list;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                filteredList.clear();
                filteredList.addAll((List<Movie>) results.values);

                clear();
                addAll(filteredList);
                notifyDataSetChanged();
            }
        };
    }

    public void updateMovies(List<Movie> movies) {
        originalList.clear();
        originalList.addAll(movies);

        filteredList.clear();
        filteredList.addAll(movies);

        clear();
        addAll(filteredList);
        notifyDataSetChanged();
    }

    private OnQuickActionClickListener quickActionListener;
    private boolean showQuickAction = false;

    public interface OnQuickActionClickListener {
        void onQuickActionClick(Movie movie);
    }

    public void setOnQuickActionClickListener(OnQuickActionClickListener listener) {
        this.quickActionListener = listener;
    }

    public void setShowQuickAction(boolean showQuickAction) {
        this.showQuickAction = showQuickAction;
        notifyDataSetChanged();
    }

}