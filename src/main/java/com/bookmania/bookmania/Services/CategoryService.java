package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.CategoryRequest;
import com.bookmania.bookmania.Dtos.CategoryResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Category;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
import com.bookmania.bookmania.Repository.BookRepository;
import com.bookmania.bookmania.Repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getDescription()))
                .toList();
    }

    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved.getId(), saved.getName(), saved.getDescription());
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved.getId(), saved.getName(), saved.getDescription());
    }

    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        for (Book book : new HashSet<>(category.getBooks())) {
            book.getCategories().remove(category);
            bookRepository.save(book);
        }

        categoryRepository.delete(category);
    }
}