package lk.jiat.blockbuster.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Locale;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.model.CartItem;
import lk.jiat.blockbuster.model.Dvd;
import lk.jiat.blockbuster.model.Movie;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<CartItem> cartItems;
    private OnQuantityChangeListener changeListener;
    private OnRemoveListener removeListener;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    public CartAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.changeListener = listener;
    }

    public void setOnRemoveListener(OnRemoveListener listener) {
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public CartAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartAdapter.ViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        loadDvd(holder, cartItem);
    }

    private void loadDvd(ViewHolder holder, CartItem cartItem) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("dvds")
                .document(cartItem.getDvdId())
                .get()
                .addOnSuccessListener(dvdSnapshot -> {

                    if (!dvdSnapshot.exists()) return;

                    Dvd dvd = dvdSnapshot.toObject(Dvd.class);
                    if (dvd == null) return;

                    int position = holder.getAbsoluteAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return;

                    loadMovie(holder, cartItem, dvd, position);
                });
    }

    private void loadMovie(ViewHolder holder, CartItem cartItem, Dvd dvd, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("movies")
                .document(dvd.getMovieId())
                .get()
                .addOnSuccessListener(movieSnapshot -> {

                    if (!movieSnapshot.exists()) return;

                    Movie movie = movieSnapshot.toObject(Movie.class);
                    if (movie == null) return;

                    bindData(holder, cartItem, dvd, movie, position);
                    setupListeners(holder, cartItem, dvd, position);
                });
    }

    private void bindData(ViewHolder holder, CartItem cartItem, Dvd dvd, Movie movie, int position) {

        holder.productTitle.setText(movie.getTitle());

        holder.productPrice.setText(
                String.format(Locale.US, "LKR %,.2f", dvd.getPrice())
        );

        holder.productQuantity.setText(
                String.valueOf(cartItem.getQuantity())
        );

        if (movie.getDownloadUri() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(movie.getDownloadUri())
                    .placeholder(R.drawable.movie_24px)
                    .error(R.drawable.movie_24px)
                    .centerCrop()
                    .into(holder.productImage);

        } else if (movie.getImages() != null && !movie.getImages().isEmpty()) {

            StorageReference ref = storage.getReference(movie.getImages().get(0));

            ref.getDownloadUrl()
                    .addOnSuccessListener(uri -> {

                        int pos = holder.getBindingAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;

                        CartItem currentItem = cartItems.get(pos);
                        movie.setDownloadUri(uri.toString());

                        Glide.with(holder.itemView.getContext())
                                .load(uri)
                                .placeholder(R.drawable.movie_24px)
                                .error(R.drawable.movie_24px)
                                .centerCrop()
                                .into(holder.productImage);

                    })
                    .addOnFailureListener(e ->
                            Glide.with(holder.itemView.getContext())
                                    .load(R.drawable.movie_24px)
                                    .centerCrop()
                                    .into(holder.productImage)
                    );
        }
    }

    private void setupListeners(ViewHolder holder, CartItem cartItem, Dvd dvd, int position) {

        holder.btnPlus.setOnClickListener(v -> {
            if (cartItem.getQuantity() < dvd.getStockCount()) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                notifyItemChanged(position);

                if (changeListener != null) {
                    changeListener.onChanged(cartItem);
                }
            }
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (cartItem.getQuantity() > 1) {
                cartItem.setQuantity(cartItem.getQuantity() - 1);
                notifyItemChanged(position);

                if (changeListener != null) {
                    changeListener.onChanged(cartItem);
                }
            }
        });

        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAbsoluteAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && removeListener != null) {
                removeListener.onRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productTitle;
        TextView productPrice;
        TextView productQuantity;
        ImageView btnPlus;
        ImageView btnMinus;
        ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.item_cart_image);
            productTitle = itemView.findViewById(R.id.item_cart_title);
            productPrice = itemView.findViewById(R.id.item_cart_price);
            productQuantity = itemView.findViewById(R.id.item_cart_quantity);
            btnPlus = itemView.findViewById(R.id.item_cart_btn_plus);
            btnMinus = itemView.findViewById(R.id.item_cart_btn_minus);
            btnRemove = itemView.findViewById(R.id.item_cart_remove);
        }
    }

    public interface OnQuantityChangeListener {
        void onChanged(CartItem cartItem);
    }

    public interface OnRemoveListener {
        void onRemoved(int position);
    }
}

