package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.BookRequest;
import com.bookmania.bookmania.Dtos.BookResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Category;
import com.bookmania.bookmania.Exception.BusinessException;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
import com.bookmania.bookmania.Repository.BookRepository;
import com.bookmania.bookmania.Repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    public List<BookResponse> getAll() {
        return bookRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<BookResponse> getFiltered(String title, String author, Long categoryId,
            int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        return bookRepository.findWithFilters(title, author, categoryId, pageable)
                .map(this::toResponse);
    }

    public BookResponse getById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
        return toResponse(book);
    }

    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException("Ya existe un libro con ese ISBN");
        }

        Set<Category> categories = new HashSet<>(
            categoryRepository.findAllById(request.getCategoryIds())
        );
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron categorías válidas");
        }

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublishYear(request.getPublishYear());
        book.setCoverUrl(request.getCoverUrl());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies());
        book.setCategories(categories);

        return toResponse(bookRepository.save(book));
    }

    public BookResponse update(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));

        Set<Category> categories = new HashSet<>(
            categoryRepository.findAllById(request.getCategoryIds())
        );
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron categorías válidas");
        }

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublishYear(request.getPublishYear());
        book.setCoverUrl(request.getCoverUrl());
        book.setTotalCopies(request.getTotalCopies());
        book.setCategories(categories);

        return toResponse(bookRepository.save(book));
    }

    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Libro no encontrado");
        }
        bookRepository.deleteById(id);
    }

    private BookResponse toResponse(Book book) {
        Set<String> categoryNames = book.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublishYear(),
                book.getCoverUrl(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                categoryNames
        );
    }
}