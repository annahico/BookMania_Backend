package com.bookmania.bookmania.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CategoryRequest {
    @NotBlank
    private String name;
    private String description;
}