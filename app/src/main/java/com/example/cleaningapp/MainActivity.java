package com.example.cleaningapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView servicesRecyclerView;
    private FloatingActionButton fabCreateApplication;
    private Button btnAdminLogin;
    private List<Service> servicesList = new ArrayList<>();
    private ServiceAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        // Настройка Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Добавляем отступ для статус бара
        addStatusBarPadding();

        servicesRecyclerView = findViewById(R.id.services_recycler_view);
        fabCreateApplication = findViewById(R.id.fab_create_application);
        btnAdminLogin = findViewById(R.id.btn_admin_login);

        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ServiceAdapter(servicesList);
        servicesRecyclerView.setAdapter(adapter);

        loadServicesFromFirestore();

        fabCreateApplication.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateOrderActivity.class);
            startActivity(intent);
        });

        btnAdminLogin.setOnClickListener(v -> showAdminLoginDialog());
    }

    // Метод для добавления отступа под статус бар
    private void addStatusBarPadding() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            // Получаем высоту статус бара
            int statusBarHeight = getStatusBarHeight();

            // Устанавливаем отступ сверху для Toolbar
            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    statusBarHeight,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom()
            );
        }
    }

    // Получение высоты статус бара
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        // Если не получили высоту, используем стандартное значение
        if (result == 0) {
            result = (int) (24 * getResources().getDisplayMetrics().density); // 24dp в пиксели
        }
        return result;
    }

    private void loadServicesFromFirestore() {
        db.collection("services")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        servicesList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            Service service = new Service();
                            service.setId(document.getId());

                            String name = document.getString("name");
                            String description = document.getString("description");

                            service.setName(name != null ? name : "");
                            service.setDescription(description != null ? description : "");

                            Object priceObj = document.get("price");
                            if (priceObj instanceof Long) {
                                service.setPrice(((Long) priceObj).doubleValue());
                            } else if (priceObj instanceof Double) {
                                service.setPrice((Double) priceObj);
                            } else {
                                service.setPrice(0.0);
                            }

                            Object durationObj = document.get("duration");
                            if (durationObj instanceof Long) {
                                service.setDuration(((Long) durationObj).intValue());
                            } else if (durationObj instanceof Integer) {
                                service.setDuration((Integer) durationObj);
                            } else {
                                service.setDuration(0);
                            }

                            servicesList.add(service);
                        }
                        adapter.notifyDataSetChanged();

                        if (servicesList.isEmpty()) {
                            Toast.makeText(MainActivity.this,
                                    "Список услуг пуст",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Ошибка загрузки услуг",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAdminLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Вход администратора");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_login, null);
        builder.setView(dialogView);

        builder.setPositiveButton("Войти", (dialog, which) -> {
            android.widget.EditText etCode = dialogView.findViewById(R.id.et_admin_code);
            String enteredCode = etCode.getText().toString().trim();
            checkAdminCode(enteredCode);
        });
        builder.setNegativeButton("Отмена", null);

        builder.show();
    }

    private void checkAdminCode(String enteredCode) {
        db.collection("admins").document("admin_config")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String storedCode = task.getResult().getString("code");
                        if (storedCode != null && enteredCode.equals(storedCode.trim())) {
                            SharedPreferences prefs = getSharedPreferences("admin_prefs", MODE_PRIVATE);
                            prefs.edit().putBoolean("is_admin", true).apply();

                            Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Неверный код",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Ошибка проверки кода",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadServicesFromFirestore();
    }
}