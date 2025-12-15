package com.example.cleaningapp;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicesFragment extends Fragment {
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private AdminServicesAdapter adapter;
    private List<Service> services = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_services, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recycler_services);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminServicesAdapter(services, this::deleteService, this::editService);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_service);
        fab.setOnClickListener(v -> showAddServiceDialog());

        loadServices();
        return view;
    }

    private void loadServices() {
        db.collection("services")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) return;

                    services.clear();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Service service = new Service();
                            service.setId(doc.getId());

                            if (doc.getString("name") != null) {
                                service.setName(doc.getString("name"));
                            }
                            if (doc.getString("description") != null) {
                                service.setDescription(doc.getString("description"));
                            }

                            if (doc.get("price") != null) {
                                try {
                                    Object priceObj = doc.get("price");
                                    if (priceObj instanceof Long) {
                                        service.setPrice(((Long) priceObj).doubleValue());
                                    } else if (priceObj instanceof Double) {
                                        service.setPrice((Double) priceObj);
                                    }
                                } catch (Exception ex) {
                                    service.setPrice(0.0);
                                }
                            }

                            // ИСПРАВЛЕНО: duration вместо durationMinutes
                            if (doc.get("duration") != null) {
                                try {
                                    Object durationObj = doc.get("duration");
                                    if (durationObj instanceof Long) {
                                        service.setDuration(((Long) durationObj).intValue());
                                    } else if (durationObj instanceof Integer) {
                                        service.setDuration((Integer) durationObj);
                                    }
                                } catch (Exception ex) {
                                    service.setDuration(0);
                                }
                            }

                            services.add(service);
                        }
                    }
                    adapter.setServices(services);
                });
    }

    private void deleteService(String serviceId) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удаление услуги")
                .setMessage("Вы уверены, что хотите удалить эту услугу?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    db.collection("services").document(serviceId)
                            .delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(getContext(), "Услуга удалена", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void editService(Service service) {
        showEditServiceDialog(service);
    }

    private void showAddServiceDialog() {
        showServiceDialog(null);
    }

    private void showEditServiceDialog(Service service) {
        showServiceDialog(service);
    }

    private void showServiceDialog(Service existingService) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(existingService == null ? "Добавить услугу" : "Редактировать услугу");

        View view = getLayoutInflater().inflate(R.layout.dialog_service_edit, null);
        EditText etName = view.findViewById(R.id.et_service_name);
        EditText etDescription = view.findViewById(R.id.et_service_description);
        EditText etPrice = view.findViewById(R.id.et_service_price);
        EditText etDuration = view.findViewById(R.id.et_service_duration);

        if (existingService != null) {
            etName.setText(existingService.getName());
            etDescription.setText(existingService.getDescription());
            etPrice.setText(String.valueOf(existingService.getPrice()));
            // ИСПРАВЛЕНО: getDuration() вместо getDurationMinutes()
            etDuration.setText(String.valueOf(existingService.getDuration()));
        }

        builder.setView(view);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String durationStr = etDuration.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || durationStr.isEmpty()) {
                Toast.makeText(getContext(), "Заполните все обязательные поля", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                int duration = Integer.parseInt(durationStr);

                if (existingService == null) {
                    addNewService(name, description, price, duration);
                } else {
                    updateService(existingService.getId(), name, description, price, duration);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Введите корректные числа", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);

        builder.show();
    }

    private void addNewService(String name, String description, double price, int duration) {
        Map<String, Object> service = new HashMap<>();
        service.put("name", name);
        service.put("description", description);
        service.put("price", price);
        // ИСПРАВЛЕНО: duration вместо durationMinutes
        service.put("duration", duration);

        db.collection("services")
                .add(service)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Услуга добавлена", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateService(String serviceId, String name, String description, double price, int duration) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", description);
        updates.put("price", price);
        // ИСПРАВЛЕНО: duration вместо durationMinutes
        updates.put("duration", duration);

        db.collection("services").document(serviceId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Услуга обновлена", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Адаптер для услуг в админ-панели
    public static class AdminServicesAdapter extends RecyclerView.Adapter<AdminServicesAdapter.ViewHolder> {
        private List<Service> services;
        private DeleteServiceListener deleteListener;
        private EditServiceListener editListener;

        public AdminServicesAdapter(List<Service> services, DeleteServiceListener deleteListener,
                                    EditServiceListener editListener) {
            this.services = services;
            this.deleteListener = deleteListener;
            this.editListener = editListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_service_admin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Service service = services.get(position);

            holder.tvName.setText(service.getName());
            holder.tvDescription.setText(service.getDescription());
            holder.tvPrice.setText(service.getPrice() + " руб.");
            // ИСПРАВЛЕНО: getDuration() вместо getDurationMinutes()
            holder.tvDuration.setText(service.getDuration() + " мин");

            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteService(service.getId());
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (editListener != null) {
                    editListener.onEditService(service);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return services.size();
        }

        public void setServices(List<Service> services) {
            this.services = services;
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDescription, tvPrice, tvDuration;
            Button btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_service_name);
                tvDescription = itemView.findViewById(R.id.tv_service_description);
                tvPrice = itemView.findViewById(R.id.tv_service_price);
                tvDuration = itemView.findViewById(R.id.tv_service_duration);
                btnDelete = itemView.findViewById(R.id.btn_delete_service);
            }
        }
    }

    interface DeleteServiceListener {
        void onDeleteService(String serviceId);
    }

    interface EditServiceListener {
        void onEditService(Service service);
    }
}