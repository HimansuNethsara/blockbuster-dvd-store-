package lk.jiat.blockbuster.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.adapter.OrderAdapter;
import lk.jiat.blockbuster.databinding.FragmentOrdersBinding;
import lk.jiat.blockbuster.model.Order;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private List<Order> orderList = new ArrayList<>();
    private OrderAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        loadOrders();
    }

    private void setupRecyclerView() {

        adapter = new OrderAdapter(orderList, order -> {

            Bundle bundle = new Bundle();
            bundle.putString("orderId", order.getOrderId());

            OrderDetailsFragment fragment = new OrderDetailsFragment();
            fragment.setArguments(bundle);

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.ordersRecyclerView.setAdapter(adapter);
    }

    private void loadOrders() {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            binding.emptyOrdersView.setVisibility(View.VISIBLE);
            binding.ordersRecyclerView.setVisibility(View.GONE);
            return;
        }

        String userId = currentUser.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    orderList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        orderList.add(order);
                    }

                    adapter.notifyDataSetChanged();
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    // Optional: handle error
                    binding.emptyOrdersView.setVisibility(View.VISIBLE);
                    binding.ordersRecyclerView.setVisibility(View.GONE);
                });
    }

    private void updateUI() {

        if (orderList.isEmpty()) {
            binding.emptyOrdersView.setVisibility(View.VISIBLE);
            binding.ordersRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyOrdersView.setVisibility(View.GONE);
            binding.ordersRecyclerView.setVisibility(View.VISIBLE);
        }

        binding.ordersCount.setText(orderList.size() + " orders");
    }
}