package lk.jiat.blockbuster.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.adapter.OrderItemsAdapter;
import lk.jiat.blockbuster.databinding.FragmentOrderDetailsBinding;
import lk.jiat.blockbuster.model.Order;

public class OrderDetailsFragment extends Fragment {

    private FragmentOrderDetailsBinding binding;
    private List<Order.OrderItem> itemList = new ArrayList<>();
    private OrderItemsAdapter adapter;

    private String orderId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            orderId = getArguments().getString("orderId");
        }

        setupRecycler();
        loadOrderDetails();

        binding.btnContinueShopping.setOnClickListener(v -> {
            GenreFragment genreFragment = new GenreFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, genreFragment)
                    .addToBackStack(null)
                    .commit();
        });

    }

    private void setupRecycler() {
        adapter = new OrderItemsAdapter(itemList);
        binding.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.orderItemsRecyclerView.setAdapter(adapter);
    }

    private void loadOrderDetails() {

        FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("orderId", orderId)
                .get()
                .addOnSuccessListener(qds -> {

                    if (!qds.isEmpty()) {

                        Order order = qds.getDocuments().get(0).toObject(Order.class);

                        binding.orderIdText.setText("Order #" + order.getOrderId());
                        binding.orderTotal.setText("Rs " + order.getTotalAmount());

                        Order.Address addr = order.getShippingAddress();

                        binding.shippingName.setText(addr.getFirstname() + " " + addr.getLastname());
                        binding.shippingPhone.setText(addr.getContact());
                        binding.shippingEmail.setText(addr.getEmail());
                        binding.shippingAddress.setText(
                                addr.getAddress1() + ", " +
                                        addr.getCity() + ", " +
                                        addr.getPostcode()
                        );

                        itemList.clear();
                        itemList.addAll(order.getOrderItems());
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}