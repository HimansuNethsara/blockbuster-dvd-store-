package lk.jiat.blockbuster.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import lk.jiat.blockbuster.databinding.ItemOrderBinding;
import lk.jiat.blockbuster.model.Order;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(List<Order> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {

        Order order = orderList.get(position);

        holder.binding.orderId.setText("#" + order.getOrderId());
        holder.binding.orderStatus.setText(order.getStatus());

        if (order.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.binding.orderDate.setText(sdf.format(order.getOrderDate().toDate()));
        }

        holder.binding.orderTotalAmount.setText("Rs " + order.getTotalAmount());

        holder.itemView.setOnClickListener(v -> listener.onOrderClick(order));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        ItemOrderBinding binding;

        public OrderViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}