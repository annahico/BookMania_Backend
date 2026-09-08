package com.bookmania.bookmania;

import com.bookmania.bookmania.Dtos.CategoryRequest;
import com.bookmania.bookmania.Dtos.CategoryResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Category;
import com.bookmania.bookmania.Exception.BusinessException;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
import com.bookmania.bookmania.Repository.BookRepository;
import com.bookmania.bookmania.Repository.CategoryRepository;
import com.bookmania.bookmania.Services.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryRequest request;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1L).name("Ficción").description("Novelas").build();

        request = new CategoryRequest();
        request.setName("Ficción");
        request.setDescription("Novelas");
    }

    @Test
    void getAll_returnsAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryResponse> result = categoryService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Ficción");
    }

    @Test
    void getById_existing_returnsCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse result = categoryService.getById(1L);

        assertThat(result.getName()).isEqualTo("Ficción");
    }

    @Test
    void getById_missing_throwsResourceNotFoundException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_newName_savesCategory() {
        when(categoryRepository.existsByName("Ficción")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse result = categoryService.create(request);

        assertThat(result.getName()).isEqualTo("Ficción");
    }

    @Test
    void create_duplicateName_throwsBusinessException() {
        when(categoryRepository.existsByName("Ficción")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void update_existing_updatesNameAndDescription() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        request.setName("Fantasía");
        CategoryResponse result = categoryService.update(1L, request);

        assertThat(result.getName()).isEqualTo("Fantasía");
    }

    @Test
    void update_missing_throwsResourceNotFoundException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesCategoryFromItsBooksThenDeletesIt() {
        Book book = Book.builder().id(5L).title("Some Book").build();
        Set<Category> bookCategories = new HashSet<>(Set.of(category));
        book.setCategories(bookCategories);
        category.setBooks(new HashSet<>(Set.of(book)));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L);

        assertThat(book.getCategories()).doesNotContain(category);
        verify(bookRepository).save(book);
        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_missing_throwsResourceNotFoundException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
