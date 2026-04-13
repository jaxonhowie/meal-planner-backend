package com.mealplanner.dto;

import lombok.Data;

@Data
public class DishStat {
    private String dishName;
    private double avgRating;
    private int checkinCount;
}
