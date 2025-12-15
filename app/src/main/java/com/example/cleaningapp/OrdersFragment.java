package com.example.cleaningapp;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private List<Order> orders = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recycler_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrdersAdapter(orders, this::updateOrderStatus);
        recyclerView.setAdapter(adapter);

        loadOrders();
        return view;
    }

    private void loadOrders() {
        db.collection("orders")
                .orderBy("createdAt")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Ошибка загрузки: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    orders.clear();
                    if (querySnapshot != null) {
                        // ИСПРАВЛЕННАЯ СТРОКА: используем DocumentSnapshot вместо QueryDocumentSnapshot
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Order order = new Order();
                            order.setId(doc.getId());

                            if (doc.getString("customerName") != null) {
                                order.setCustomerName(doc.getString("customerName"));
                            }
                            if (doc.getString("customerPhone") != null) {
                                order.setCustomerPhone(doc.getString("customerPhone"));
                            }
                            if (doc.getString("serviceName") != null) {
                                order.setServiceName(doc.getString("serviceName"));
                            }
                            if (doc.getString("date") != null) {
                                order.setDate(doc.getString("date"));
                            }
                            if (doc.getString("time") != null) {
                                order.setTime(doc.getString("time"));
                            }
                            if (doc.getString("status") != null) {
                                order.setStatus(doc.getString("status"));
                            }

                            orders.add(order);
                        }
                    }
                    adapter.setOrders(orders);
                });
    }

    private void updateOrderStatus(String orderId, String status) {
        db.collection("orders").document(orderId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    String message = status.equals("accepted") ? "Заявка принята" : "Заявка отклонена";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                    // Если отклонили - удаляем заявку
                    if (status.equals("rejected")) {
                        db.collection("orders").document(orderId).delete();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Адаптер для заявок
    public static class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {
        private List<Order> orders;
        private OrderStatusListener listener;

        public OrdersAdapter(List<Order> orders, OrderStatusListener listener) {
            this.orders = orders;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_admin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Order order = orders.get(position);

            // Устанавливаем данные
            holder.tvInfo.setText("Имя: " + order.getCustomerName() +
                    "\nТелефон: " + order.getCustomerPhone());
            holder.tvService.setText("Услуга: " + order.getServiceName());
            holder.tvDateTime.setText(order.getDate() + " в " + order.getTime());

            // Статус
            String statusText = getStatusText(order.getStatus());
            holder.tvStatus.setText(statusText);
            holder.tvStatus.setBackgroundResource(getStatusColor(order.getStatus()));

            // Показываем/скрываем кнопки действий
            if ("pending".equals(order.getStatus())) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnAccept.setOnClickListener(v ->
                        listener.onStatusChange(order.getId(), "accepted"));
                holder.btnReject.setOnClickListener(v ->
                        listener.onStatusChange(order.getId(), "rejected"));
            } else {
                holder.layoutActions.setVisibility(View.GONE);
            }
        }

        private String getStatusText(String status) {
            switch (status) {
                case "pending": return "В ожидании";
                case "accepted": return "Принята";
                case "rejected": return "Отклонена";
                default: return "Неизвестно";
            }
        }

        private int getStatusColor(String status) {
            switch (status) {
                case "pending": return R.color.pending_color;
                case "accepted": return R.color.accept_color;
                case "rejected": return R.color.reject_color;
                default: return R.color.gray;
            }
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        public void setOrders(List<Order> orders) {
            this.orders = orders;
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStatus, tvInfo, tvService, tvDateTime;
            View layoutActions;
            Button btnAccept, btnReject;

            ViewHolder(View itemView) {
                super(itemView);
                tvStatus = itemView.findViewById(R.id.tv_order_status);
                tvInfo = itemView.findViewById(R.id.tv_order_info);
                tvService = itemView.findViewById(R.id.tv_order_service);
                tvDateTime = itemView.findViewById(R.id.tv_order_datetime);
                layoutActions = itemView.findViewById(R.id.layout_actions);
                btnAccept = itemView.findViewById(R.id.btn_accept);
                btnReject = itemView.findViewById(R.id.btn_reject);
            }
        }
    }

    interface OrderStatusListener {
        void onStatusChange(String orderId, String status);
    }
}