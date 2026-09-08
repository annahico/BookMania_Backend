package com.bookmania.bookmania;

import com.bookmania.bookmania.Dtos.BookRequest;
import com.bookmania.bookmania.Dtos.BookResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Category;
import com.bookmania.bookmania.Exception.BusinessException;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
import com.bookmania.bookmania.Repository.BookRepository;
import com.bookmania.bookmania.Repository.CategoryRepository;
import com.bookmania.bookmania.Services.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BookService bookService;

    private Book book;
    private Category category;
    private BookRequest request;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(2L).name("Ficción").build();

        book = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("978-0132350884")
                .totalCopies(3)
                .availableCopies(3)
                .categories(Set.of(category))
                .build();

        request = new BookRequest();
        request.setTitle("Clean Code");
        request.setAuthor("Robert C. Martin");
        request.setIsbn("978-0132350884");
        request.setTotalCopies(3);
        request.setCategoryIds(Set.of(2L));
    }

    @Test
    void getById_existing_returnsBookWithCategoryNames() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookResponse result = bookService.getById(1L);

        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getCategories()).containsExactly("Ficción");
    }

    @Test
    void getById_missing_throwsResourceNotFoundException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_newIsbn_savesBookWithAvailableCopiesEqualToTotal() {
        when(bookRepository.existsByIsbn("978-0132350884")).thenReturn(false);
        when(categoryRepository.findAllById(Set.of(2L))).thenReturn(List.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookResponse result = bookService.create(request);

        assertThat(result.getAvailableCopies()).isEqualTo(3);
        assertThat(result.getTotalCopies()).isEqualTo(3);
    }

    @Test
    void create_duplicateIsbn_throwsBusinessException() {
        when(bookRepository.existsByIsbn("978-0132350884")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ISBN");
    }

    @Test
    void create_noValidCategories_throwsResourceNotFoundException() {
        when(bookRepository.existsByIsbn("978-0132350884")).thenReturn(false);
        when(categoryRepository.findAllById(Set.of(2L))).thenReturn(List.of());

        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_existing_updatesFieldsButKeepsAvailableCopiesUntouched() {
        book.setAvailableCopies(1); // one currently on loan
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(categoryRepository.findAllById(Set.of(2L))).thenReturn(List.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        request.setTitle("Clean Code (2nd ed.)");
        BookResponse result = bookService.update(1L, request);

        assertThat(result.getTitle()).isEqualTo("Clean Code (2nd ed.)");
        assertThat(result.getAvailableCopies()).isEqualTo(1);
    }

    @Test
    void update_missing_throwsResourceNotFoundException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_existing_deletesBook() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.delete(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    void delete_missing_throwsResourceNotFoundException() {
        when(bookRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bookRepository, never()).deleteById(any());
    }
}
