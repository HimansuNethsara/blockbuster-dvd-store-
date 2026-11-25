package lk.jiat.blockbuster.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.jiat.blockbuster.databinding.FragmentCheckoutBinding;
import lk.jiat.blockbuster.listener.FirestoreCallback;
import lk.jiat.blockbuster.model.CartItem;
import lk.jiat.blockbuster.model.Dvd;
import lk.jiat.blockbuster.model.Order;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;
import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private List<CartItem> cartItems = new ArrayList<>();
    private Map<String, Dvd> dvdMap = new HashMap<>();

    private double subtotal = 0;
    private double shipping = 250;
    private double total = 0;

    private boolean paymentActive = false;

    private boolean isShippingExpanded = true;
    private boolean isBillingExpanded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentCheckoutBinding.inflate(inflater, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        setupToggles();
        setupBillingCheckbox();
        loadCart();

        binding.checkoutBtnProceed.setOnClickListener(v -> startPayment());
    }

    private void loadCart() {

        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        getCartItems(uid, cart -> {

            cartItems.clear();
            cartItems.addAll(cart);

            if (cartItems.isEmpty()) {
                paymentActive = false;
                binding.checkoutSubtotal.setText("LKR 0.00");
                binding.checkoutShipping.setText("LKR 0.00");
                binding.checkoutTotal.setText("LKR 0.00");
                return;
            }

            loadDvds();
        });
    }

    private void loadDvds() {

        List<String> dvdIds = new ArrayList<>();

        for (CartItem item : cartItems) {
            dvdIds.add(item.getDvdId());
        }

        getDvdsByIds(dvdIds, map -> {

            dvdMap.clear();
            dvdMap.putAll(map);

            subtotal = 0;
            paymentActive = true;

            for (CartItem item : cartItems) {

                Dvd dvd = dvdMap.get(item.getDvdId());

                if (dvd == null) {
                    paymentActive = false;
                    Toast.makeText(getContext(),
                            "Item not found",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (dvd.getStockCount() < item.getQuantity()) {
                    paymentActive = false;
                    Toast.makeText(getContext(),
                            dvd.getDvdId() + " is out of stock",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                subtotal += dvd.getPrice() * item.getQuantity();
            }

            total = subtotal + shipping;

            binding.checkoutSubtotal.setText(
                    String.format(Locale.US, "LKR %,.2f", subtotal)
            );

            binding.checkoutShipping.setText(
                    String.format(Locale.US, "LKR %,.2f", shipping)
            );

            binding.checkoutTotal.setText(
                    String.format(Locale.US, "LKR %,.2f", total)
            );

        });
    }

    private void getCartItems(String uid, FirestoreCallback<List<CartItem>> callback) {

        db.collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(qds -> {

                    List<CartItem> list = new ArrayList<>();

                    for (DocumentSnapshot ds : qds.getDocuments()) {
                        CartItem item = ds.toObject(CartItem.class);
                        if (item != null) list.add(item);
                    }

                    callback.onCallback(list);
                });
    }

    private void getDvdsByIds(List<String> dvdIds, FirestoreCallback<Map<String, Dvd>> callback) {

        Map<String, Dvd> map = new HashMap<>();

        if (dvdIds == null || dvdIds.isEmpty()) {
            callback.onCallback(map);
            return;
        }

        db.collection("dvds")
                .whereIn("dvdId", dvdIds)
                .get()
                .addOnSuccessListener(qds -> {

                    for (DocumentSnapshot ds : qds.getDocuments()) {
                        Dvd dvd = ds.toObject(Dvd.class);
                        if (dvd != null) {
                            map.put(dvd.getDvdId(), dvd);
                        }
                    }

                    callback.onCallback(map);
                });
    }

    private void startPayment() {

        binding.checkoutBtnProceed.setEnabled(false);

        if (!paymentActive || cartItems.isEmpty()) {
            binding.checkoutBtnProceed.setEnabled(true);
            return;
        }

        String firstName = binding.shippingDetailsFirstname.getText().toString().trim();
        String lastName = binding.shippingDetailsLastname.getText().toString().trim();
        String email = binding.shippingDetailsEmail.getText().toString().trim();
        String phone = binding.shippingDetailsContact.getText().toString().trim();
        String address1 = binding.shippingDetailsAddress1.getText().toString().trim();
        String address2 = binding.shippingDetailsAddress2.getText().toString().trim();
        String city = binding.shippingDetailsCity.getText().toString().trim();

        if (firstName.isEmpty() || email.isEmpty() || phone.isEmpty()
                || address1.isEmpty() || address2.isEmpty() || city.isEmpty()) {

            Toast.makeText(getContext(), "Fill shipping details", Toast.LENGTH_SHORT).show();
            binding.checkoutBtnProceed.setEnabled(true);
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Invalid email", Toast.LENGTH_SHORT).show();
            binding.checkoutBtnProceed.setEnabled(true);
            return;
        }

        if (!phone.matches("^[0-9+]{9,15}$")) {
            Toast.makeText(getContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
            binding.checkoutBtnProceed.setEnabled(true);
            return;
        }

        if (!binding.shippingDetailsCheckBilling.isChecked()) {

            if (binding.billingDetailsFirstname.getText().toString().trim().isEmpty() ||
                    binding.billingDetailsLastname.getText().toString().trim().isEmpty() ||
                    binding.billingDetailsEmail.getText().toString().trim().isEmpty() ||
                    binding.billingDetailsContact.getText().toString().trim().isEmpty() ||
                    binding.billingDetailsAddress1.getText().toString().trim().isEmpty() ||
                    binding.billingDetailsCity.getText().toString().trim().isEmpty()) {

                Toast.makeText(getContext(),
                        "Fill complete billing details",
                        Toast.LENGTH_SHORT).show();

                binding.checkoutBtnProceed.setEnabled(true);
                return;
            }
        }

        InitRequest req = new InitRequest();
        req.setSandBox(true);

        req.setMerchantId("1231397");
        req.setMerchantSecret("YOUR_PAYHERE_MERCHANT_SECRET");
        req.setCurrency("LKR");
        req.setAmount(total);
        req.setOrderId("BLOCK-" + System.currentTimeMillis());
        req.setItemsDescription("DVD Purchase");

        req.getCustomer().setFirstName(firstName);
        req.getCustomer().setLastName(lastName);
        req.getCustomer().setEmail(email);
        req.getCustomer().setPhone(phone);

        req.getCustomer().getAddress().setAddress(address1 + ", " + address2);
        req.getCustomer().getAddress().setCity(city);
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        Intent intent = new Intent(getActivity(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

        payhereLauncher.launch(intent);
    }

    @SuppressWarnings("unchecked")
    private final ActivityResultLauncher<Intent> payhereLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                    @SuppressWarnings("unchecked")
                    PHResponse<StatusResponse> response =
                            (PHResponse<StatusResponse>) result.getData()
                                    .getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                    if (response != null && response.getData() != null) {

                        StatusResponse status = response.getData();

                        if (status.getStatus() == 2) {
                            saveOrder();
                        } else {
                            Toast.makeText(getContext(),
                                    "Payment Failed (Status: " + status.getStatus() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(getContext(), "Payment Failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    binding.checkoutBtnProceed.setEnabled(true);
                }
            });

    private void saveOrder() {

        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        Order order = new Order();
        order.setUserId(uid);
        order.setTotalAmount(total);
        order.setStatus("PAID");
        order.setOrderDate(Timestamp.now());

        List<Order.OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : cartItems) {

            Dvd dvd = dvdMap.get(item.getDvdId());

            if (dvd == null) continue;

            Order.OrderItem orderItem = Order.OrderItem.builder()
                    .dvdId(item.getDvdId())
                    .quantity(item.getQuantity())
                    .unitPrice(dvd.getPrice())
                    .build();

            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);

        Order.Address shippingAddress = Order.Address.builder()
                .firstname(binding.shippingDetailsFirstname.getText().toString())
                .lastname(binding.shippingDetailsLastname.getText().toString())
                .email(binding.shippingDetailsEmail.getText().toString())
                .contact(binding.shippingDetailsContact.getText().toString())
                .address1(binding.shippingDetailsAddress1.getText().toString())
                .address2(binding.shippingDetailsAddress2.getText().toString())
                .city(binding.shippingDetailsCity.getText().toString())
                .postcode(binding.shippingDetailsPostcode.getText().toString())
                .build();

        order.setShippingAddress(shippingAddress);

        Order.Address billingAddress;

        if (binding.shippingDetailsCheckBilling.isChecked()) {

            billingAddress = shippingAddress;

        } else {

            billingAddress = Order.Address.builder()
                    .firstname(binding.billingDetailsFirstname.getText().toString())
                    .lastname(binding.billingDetailsLastname.getText().toString())
                    .email(binding.billingDetailsEmail.getText().toString())
                    .contact(binding.billingDetailsContact.getText().toString())
                    .address1(binding.billingDetailsAddress1.getText().toString())
                    .address2(binding.billingDetailsAddress2.getText().toString())
                    .city(binding.billingDetailsCity.getText().toString())
                    .postcode(binding.billingDetailsPostcode.getText().toString())
                    .build();
        }

        order.setBillingAddress(billingAddress);

        updateStockAndSaveOrder(order, uid);
    }

    private void updateStockAndSaveOrder(Order order, String uid) {

        db.runTransaction(transaction -> {

            Map<String, Integer> updatedStocks = new HashMap<>();

            for (CartItem item : cartItems) {

                String dvdId = item.getDvdId();

                DocumentSnapshot dvdSnapshot = transaction.get(
                        db.collection("dvds").document(dvdId)
                );

                Dvd dvd = dvdSnapshot.toObject(Dvd.class);

                if (dvd == null) {
                    throw new RuntimeException("DVD_NOT_FOUND");
                }

                int currentStock = dvd.getStockCount();
                int newStock = currentStock - item.getQuantity();

                if (newStock < 0) {
                    throw new RuntimeException("OUT_OF_STOCK");
                }

                updatedStocks.put(dvdId, newStock);
            }

            for (Map.Entry<String, Integer> entry : updatedStocks.entrySet()) {

                transaction.update(
                        db.collection("dvds").document(entry.getKey()),
                        "stockCount",
                        entry.getValue()
                );
            }

            String orderId = String.valueOf(System.currentTimeMillis());
            order.setOrderId(orderId);

            transaction.set(
                    db.collection("orders").document(orderId),
                    order
            );

            return null;

        }).addOnSuccessListener(unused -> {

            clearCart(uid);
            clearCheckoutFields();

            MotionToast.Companion.createColorToast(
                    requireActivity(),
                    "Success",
                    "Order placed successfully",
                    MotionToastStyle.SUCCESS,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.SHORT_DURATION,
                    Typeface.DEFAULT
            );

        }).addOnFailureListener(e -> {

            String message = e.getMessage();

            if ("DVD_NOT_FOUND".equals(message)) {
                Toast.makeText(getContext(), "DVD not found", Toast.LENGTH_LONG).show();
            } else if ("OUT_OF_STOCK".equals(message)) {
                Toast.makeText(getContext(), "Some items are out of stock", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(),
                        "Checkout failed: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearCart(String uid) {

        db.collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(qds -> {

                    List<DocumentSnapshot> docs = qds.getDocuments();

                    for (DocumentSnapshot ds : docs) {
                        ds.getReference().delete();
                    }
                });
    }

    private void setupToggles() {
        binding.shippingLayout.setOnClickListener(v -> toggleShipping());
        binding.billingLayout.setOnClickListener(v -> toggleBilling());
    }

    private void toggleShipping() {

        isShippingExpanded = !isShippingExpanded;

        binding.shippingLayoutBody.setVisibility(
                isShippingExpanded ? View.VISIBLE : View.GONE
        );

        binding.shippingLayoutBtn.animate()
                .rotation(isShippingExpanded ? 0f : 180f)
                .setDuration(200)
                .start();
    }

    private void toggleBilling() {

        isBillingExpanded = !isBillingExpanded;

        binding.billingLayoutBody.setVisibility(
                isBillingExpanded ? View.VISIBLE : View.GONE
        );

        binding.billingLayoutBtn.animate()
                .rotation(isBillingExpanded ? 0f : 180f)
                .setDuration(200)
                .start();
    }

    private void setupBillingCheckbox() {

        binding.shippingDetailsCheckBilling.setOnCheckedChangeListener(
                (CompoundButton buttonView, boolean isChecked) -> {

                    binding.billingLayout.setVisibility(
                            isChecked ? View.GONE : View.VISIBLE
                    );

                    if (isChecked) {
                        syncBillingFromShipping();
                    }
                }
        );
    }

    private void syncBillingFromShipping() {

        if (!binding.shippingDetailsCheckBilling.isChecked()) return;

        binding.billingDetailsFirstname.setText(binding.shippingDetailsFirstname.getText().toString());
        binding.billingDetailsLastname.setText(binding.shippingDetailsLastname.getText().toString());
        binding.billingDetailsEmail.setText(binding.shippingDetailsEmail.getText().toString());
        binding.billingDetailsContact.setText(binding.shippingDetailsContact.getText().toString());
        binding.billingDetailsAddress1.setText(binding.shippingDetailsAddress1.getText().toString());
        binding.billingDetailsAddress2.setText(binding.shippingDetailsAddress2.getText().toString());
        binding.billingDetailsCity.setText(binding.shippingDetailsCity.getText().toString());
        binding.billingDetailsPostcode.setText(binding.shippingDetailsPostcode.getText().toString());
    }

    private void clearCheckoutFields() {

        binding.shippingDetailsFirstname.setText("");
        binding.shippingDetailsLastname.setText("");
        binding.shippingDetailsEmail.setText("");
        binding.shippingDetailsContact.setText("");
        binding.shippingDetailsAddress1.setText("");
        binding.shippingDetailsAddress2.setText("");
        binding.shippingDetailsCity.setText("");
        binding.shippingDetailsPostcode.setText("");

        binding.billingDetailsFirstname.setText("");
        binding.billingDetailsLastname.setText("");
        binding.billingDetailsEmail.setText("");
        binding.billingDetailsContact.setText("");
        binding.billingDetailsAddress1.setText("");
        binding.billingDetailsAddress2.setText("");
        binding.billingDetailsCity.setText("");
        binding.billingDetailsPostcode.setText("");

        subtotal = 0;
        total = 0;

        binding.checkoutSubtotal.setText("LKR 0.00");
        binding.checkoutShipping.setText("LKR 0.00");
        binding.checkoutTotal.setText("LKR 0.00");

        paymentActive = false;

    }
}