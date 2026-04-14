package com.mealplanner.dto;

import lombok.Data;

@Data
public class DayCount {
    /** 日期，格式 "2026-04-01" */
    private String date;
    /** 当日打卡次数 */
    private int count;
}
