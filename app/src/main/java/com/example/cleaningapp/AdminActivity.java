package com.example.cleaningapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Проверяем, вошел ли админ
        SharedPreferences prefs = getSharedPreferences("admin_prefs", MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("is_admin", false);

        if (!isAdmin) {
            Toast.makeText(this, "Доступ запрещен", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // Настройка Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Добавляем отступ для статус бара
        addStatusBarPadding();

        // Настройка ViewPager
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        AdminPagerAdapter adapter = new AdminPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Связываем TabLayout с ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Заявки");
                    } else {
                        tab.setText("Услуги");
                    }
                }
        ).attach();

        // Кнопка выхода
        Button btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> logout());
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
            result = (int) (24 * getResources().getDisplayMetrics().density);
        }
        return result;
    }

    private void logout() {
        getSharedPreferences("admin_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("is_admin", false)
                .apply();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // Адаптер для ViewPager
    private static class AdminPagerAdapter extends FragmentStateAdapter {
        public AdminPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new OrdersFragment();
            } else {
                return new ServicesFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}