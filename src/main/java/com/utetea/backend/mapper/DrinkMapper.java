package com.utetea.backend.mapper;

import com.utetea.backend.dto.DrinkDto;
import com.utetea.backend.dto.DrinkSizeDto;
import com.utetea.backend.dto.DrinkToppingDto;
import com.utetea.backend.model.Drink;
import com.utetea.backend.model.DrinkSize;
import com.utetea.backend.model.DrinkTopping;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DrinkMapper {

    public DrinkDto toDto(Drink drink) {
        if (drink == null) {
            return null;
        }

        DrinkDto dto = new DrinkDto();
        dto.setId(drink.getId());
        dto.setName(drink.getName());
        dto.setDescription(drink.getDescription());
        dto.setImageUrl(drink.getImageUrl());
        dto.setBasePrice(drink.getBasePrice());
        dto.setIsActive(drink.getIsActive());

        if (drink.getCategory() != null) {
            dto.setCategoryId(drink.getCategory().getId());
            dto.setCategoryName(drink.getCategory().getName());
        }

        if (drink.getSizes() != null) {
            dto.setSizes(drink.getSizes().stream()
                    .map(this::toSizeDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setSizes(Collections.emptyList());
        }

        if (drink.getToppings() != null) {
            dto.setToppings(drink.getToppings().stream()
                    .map(this::toToppingDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setToppings(Collections.emptyList());
        }

        return dto;
    }

    private DrinkSizeDto toSizeDto(DrinkSize size) {
        DrinkSizeDto dto = new DrinkSizeDto();
        dto.setId(size.getId());
        dto.setSizeName(size.getSizeName());
        dto.setExtraPrice(size.getExtraPrice());
        return dto;
    }

    private DrinkToppingDto toToppingDto(DrinkTopping topping) {
        DrinkToppingDto dto = new DrinkToppingDto();
        dto.setId(topping.getId());
        dto.setToppingName(topping.getToppingName());
        dto.setPrice(topping.getPrice());
        dto.setIsActive(topping.getIsActive());
        return dto;
    }
}
