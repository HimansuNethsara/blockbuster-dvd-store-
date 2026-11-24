package lk.jiat.blockbuster.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.adapter.CartAdapter;
import lk.jiat.blockbuster.databinding.FragmentCartBinding;
import lk.jiat.blockbuster.model.CartItem;
import lk.jiat.blockbuster.model.Dvd;
import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private List<CartItem> cartItems;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadCart();

        binding.btnStartShopping.setOnClickListener(v -> {
            GenreFragment genreFragment = new GenreFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, genreFragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnProceedToShipping.setOnClickListener(v -> {
            CheckoutFragment checkoutFragment = new CheckoutFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, checkoutFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadCart() {

        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(qds -> {

                    if (qds.isEmpty()) {
                        showEmptyState(true);
                        return;
                    }

                    cartItems = new ArrayList<>();

                    for (DocumentSnapshot ds : qds.getDocuments()) {
                        CartItem item = ds.toObject(CartItem.class);
                        if (item != null) {
                            item.setDocumentId(ds.getId());
                            cartItems.add(item);
                        }
                    }

                    setupRecyclerView(uid);
                    updateTotal();

                });
    }

    private void setupRecyclerView(String uid) {

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.cartRecyclerView.setLayoutManager(layoutManager);

        CartAdapter adapter = new CartAdapter(cartItems);

        adapter.setOnQuantityChangeListener(cartItem -> db.collection("users")
                .document(uid)
                .collection("cart")
                .document(cartItem.getDocumentId())
                .update("quantity", cartItem.getQuantity())
                .addOnSuccessListener(unused -> updateTotal()));

        adapter.setOnRemoveListener(position -> {

            String docId = cartItems.get(position).getDocumentId();

            db.collection("users")
                    .document(uid)
                    .collection("cart")
                    .document(docId)
                    .delete()
                    .addOnSuccessListener(unused -> {

                        cartItems.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, cartItems.size());

                        updateTotal();
                        showEmptyState(cartItems.isEmpty());

                        MotionToast.Companion.createColorToast(
                                requireActivity(),
                                "Removed",
                                "Item removed from cart",
                                MotionToastStyle.SUCCESS,
                                MotionToast.GRAVITY_BOTTOM,
                                MotionToast.SHORT_DURATION,
                                Typeface.DEFAULT
                        );
                    });

        });

        binding.cartRecyclerView.setAdapter(adapter);

        showEmptyState(false);
    }

    private void updateTotal() {

        if (cartItems == null || cartItems.isEmpty()) {
            binding.tvTotal.setText(String.format(Locale.US, "Rs %,.2f", 0.0));
            binding.tvSubtotal.setText(String.format(Locale.US, "Rs %,.2f", 0.0));
            return;
        }

        List<String> dvdIds = new ArrayList<>();
        for (CartItem item : cartItems) {
            dvdIds.add(item.getDvdId());
        }

        db.collection("dvds")
                .whereIn("dvdId", dvdIds)
                .get()
                .addOnSuccessListener(qds -> {

                    double subtotal = 0;

                    Map<String, Dvd> dvdMap = new HashMap<>();

                    for (DocumentSnapshot ds : qds.getDocuments()) {
                        Dvd dvd = ds.toObject(Dvd.class);
                        if (dvd != null) {
                            dvdMap.put(dvd.getDvdId(), dvd);
                        }
                    }

                    for (CartItem item : cartItems) {

                        Dvd dvd = dvdMap.get(item.getDvdId());

                        if (dvd != null) {
                            subtotal += dvd.getPrice() * item.getQuantity();
                        }
                    }

                    double deliveryFee = 250.0;
                    double total = subtotal + deliveryFee;

                    binding.tvSubtotal.setText(String.format(Locale.US, "Rs %,.2f", subtotal));
                    binding.tvDeliveryFee.setText(String.format(Locale.US, "Rs %,.2f", deliveryFee));
                    binding.tvTotal.setText(String.format(Locale.US, "Rs %,.2f", total));
                });
    }

    private void showEmptyState(boolean isEmpty) {

        if (isEmpty) {
            binding.emptyCartView.setVisibility(View.VISIBLE);
            binding.cartRecyclerView.setVisibility(View.GONE);
            binding.bottomSummaryBar.setVisibility(View.GONE);
        } else {
            binding.emptyCartView.setVisibility(View.GONE);
            binding.cartRecyclerView.setVisibility(View.VISIBLE);
            binding.bottomSummaryBar.setVisibility(View.VISIBLE);
        }
    }
}