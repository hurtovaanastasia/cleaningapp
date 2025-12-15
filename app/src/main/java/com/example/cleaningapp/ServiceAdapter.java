package com.example.cleaningapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {
    private List<Service> services;

    public ServiceAdapter(List<Service> services) {
        this.services = services;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Service service = services.get(position);

        holder.tvName.setText(service.getName());
        holder.tvDescription.setText(service.getDescription());
        holder.tvPrice.setText(String.format("%d руб.", (int)service.getPrice()));

        // Форматирование времени
        int hours = service.getDuration() / 60; // ИСПРАВЛЕНО: getDuration()
        int minutes = service.getDuration() % 60;
        String timeText;
        if (hours > 0) {
            timeText = hours + " ч" + (minutes > 0 ? " " + minutes + " мин" : "");
        } else {
            timeText = minutes + " мин";
        }
        holder.tvDuration.setText(timeText);
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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_service_name);
            tvDescription = itemView.findViewById(R.id.tv_service_description);
            tvPrice = itemView.findViewById(R.id.tv_service_price);
            tvDuration = itemView.findViewById(R.id.tv_service_duration);
        }
    }
}