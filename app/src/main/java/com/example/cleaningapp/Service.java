package com.example.cleaningapp;

public class Service {
    private String id;
    private String name;
    private String description;
    private double price;
    private int duration; // ИСПРАВЛЕНО: duration вместо durationMinutes

    public Service() {}

    public Service(String name, String description, double price, int duration) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.duration = duration;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getDuration() { return duration; } // ИСПРАВЛЕНО
    public void setDuration(int duration) { this.duration = duration; } // ИСПРАВЛЕНО
}