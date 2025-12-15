package com.example.cleaningapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateOrderActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private List<Service> services = new ArrayList<>();
    private Spinner spinnerServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_order);

        // Добавляем отступ для ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        services = new ArrayList<>();

        spinnerServices = findViewById(R.id.spinner_services);

        Button btnSubmit = findViewById(R.id.btn_submit_order);
        btnSubmit.setOnClickListener(v -> createOrder());

        loadServicesFromFirestore();
    }

    private void loadServicesFromFirestore() {
        db.collection("services")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        services.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            Service service = new Service();
                            service.setId(document.getId());

                            if (document.getString("name") != null) {
                                service.setName(document.getString("name"));
                            }

                            if (document.getString("description") != null) {
                                service.setDescription(document.getString("description"));
                            }

                            if (document.get("price") != null) {
                                try {
                                    Object priceObj = document.get("price");
                                    if (priceObj instanceof Long) {
                                        service.setPrice(((Long) priceObj).doubleValue());
                                    } else if (priceObj instanceof Double) {
                                        service.setPrice((Double) priceObj);
                                    }
                                } catch (Exception e) {
                                    service.setPrice(0.0);
                                }
                            }

                            // ИСПРАВЛЕНО: duration вместо durationMinutes
                            if (document.get("duration") != null) {
                                try {
                                    Object durationObj = document.get("duration");
                                    if (durationObj instanceof Long) {
                                        service.setDuration(((Long) durationObj).intValue());
                                    } else if (durationObj instanceof Integer) {
                                        service.setDuration((Integer) durationObj);
                                    }
                                } catch (Exception e) {
                                    service.setDuration(0);
                                }
                            }

                            services.add(service);
                        }

                        setupSpinner();
                    } else {
                        Toast.makeText(this,
                                "Ошибка загрузки услуг",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupSpinner() {
        if (services.isEmpty()) {
            Toast.makeText(this, "Нет доступных услуг", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> serviceNames = new ArrayList<>();
        for (Service service : services) {
            String text = service.getName() + " - " + service.getPrice() + " руб.";
            serviceNames.add(text);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, serviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerServices.setAdapter(adapter);
    }

    private void createOrder() {
        if (services.isEmpty()) {
            Toast.makeText(this, "Список услуг пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = spinnerServices.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= services.size()) {
            Toast.makeText(this, "Выберите услугу", Toast.LENGTH_SHORT).show();
            return;
        }

        Service selectedService = services.get(selectedPosition);

        EditText etName = findViewById(R.id.et_customer_name);
        EditText etPhone = findViewById(R.id.et_customer_phone);
        EditText etEmail = findViewById(R.id.et_customer_email);
        DatePicker datePicker = findViewById(R.id.date_picker);
        TimePicker timePicker = findViewById(R.id.time_picker);

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Заполните имя и телефон", Toast.LENGTH_SHORT).show();
            return;
        }

        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth() + 1;
        int year = datePicker.getYear();
        String date = String.format("%02d.%02d.%d", day, month, year);

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String time = String.format("%02d:%02d", hour, minute);

        Map<String, Object> order = new HashMap<>();
        order.put("serviceId", selectedService.getId());
        order.put("serviceName", selectedService.getName());
        order.put("customerName", name);
        order.put("customerPhone", phone);
        order.put("customerEmail", email.isEmpty() ? null : email);
        order.put("date", date);
        order.put("time", time);
        order.put("status", "pending");
        order.put("createdAt", new Timestamp(new Date()));

        db.collection("orders")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Заявка создана!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Обработка нажатия кнопки "Назад" в ActionBar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}