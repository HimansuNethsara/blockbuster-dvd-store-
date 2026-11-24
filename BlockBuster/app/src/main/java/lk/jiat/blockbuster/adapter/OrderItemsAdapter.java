package lk.jiat.blockbuster.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.databinding.ItemOrderItemBinding;
import lk.jiat.blockbuster.model.Dvd;
import lk.jiat.blockbuster.model.Movie;
import lk.jiat.blockbuster.model.Order;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {

    private List<Order.OrderItem> list;

    public OrderItemsAdapter(List<Order.OrderItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderItemBinding binding = ItemOrderItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Order.OrderItem item = list.get(position);

        holder.binding.orderItemQuantity.setText(String.valueOf(item.getQuantity()));
        holder.binding.orderItemPrice.setText("Rs " + item.getUnitPrice());

        holder.binding.orderItemTitle.setText("Loading...");
        holder.binding.orderItemImage.setImageResource(R.drawable.movie_24px);

        loadDvd(holder, item);

    }

    private void loadDvd(ViewHolder holder, Order.OrderItem item) {

        FirebaseFirestore.getInstance()
                .collection("dvds")
                .document(item.getDvdId())
                .get()
                .addOnSuccessListener(dvdSnapshot -> {

                    if (!dvdSnapshot.exists()) return;

                    Dvd dvd = dvdSnapshot.toObject(Dvd.class);
                    if (dvd == null) return;

                    loadMovie(holder, item, dvd);
                });
    }

    private void loadMovie(ViewHolder holder, Order.OrderItem item, Dvd dvd) {

        FirebaseFirestore.getInstance()
                .collection("movies")
                .document(dvd.getMovieId())
                .get()
                .addOnSuccessListener(movieSnapshot -> {

                    if (!movieSnapshot.exists()) return;

                    Movie movie = movieSnapshot.toObject(Movie.class);
                    if (movie == null) return;

                    bindData(holder, item, movie);
                });
    }

    private void bindData(ViewHolder holder, Order.OrderItem item, Movie movie) {

        holder.binding.orderItemTitle.setText(movie.getTitle());

        if (movie.getImages() != null && !movie.getImages().isEmpty()) {

            String imagePath = movie.getImages().get(0);

            FirebaseStorage.getInstance()
                    .getReference()
                    .child(imagePath)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {

                        Glide.with(holder.itemView.getContext())
                                .load(uri)
                                .placeholder(R.drawable.movie_24px)
                                .into(holder.binding.orderItemImage);
                    });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ItemOrderItemBinding binding;

        public ViewHolder(ItemOrderItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}