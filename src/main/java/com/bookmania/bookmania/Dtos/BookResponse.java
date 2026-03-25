package com.bookmania.bookmania.Dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class BookResponse {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private Integer pages;
    private Integer publishYear;
    private String coverUrl;
    private Integer totalCopies;
    private Integer availableCopies;
    private Set<String> categories;
}