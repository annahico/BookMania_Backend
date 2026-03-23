package com.bookmania.bookmania.Dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class BookRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String author;
    @NotBlank
    private String isbn;
    private Integer publishYear;
    private String coverUrl;
    @NotNull
    @Min(1)
    private Integer totalCopies;
    @NotEmpty
    private Set<Long> categoryIds; 
}