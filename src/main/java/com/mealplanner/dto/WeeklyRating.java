package com.mealplanner.dto;

import lombok.Data;

@Data
public class WeeklyRating {
    /** 周起始日，格式 "MM/dd"，例如 "04/07" */
    private String weekLabel;
    private double avgRating;
    private int checkinCount;
}
