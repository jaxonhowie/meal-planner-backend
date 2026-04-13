package com.mealplanner.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateDishesRequest {
    private List<DishItem> dishes;
}
