package com.bookmania.bookmania.Repository;

import com.bookmania.bookmania.Entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    Optional<Book> findByIsbn(String isbn);

    @Query("""
        SELECT b FROM Book b
        WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
        AND (:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', CAST(:author AS string), '%')))
        AND (:categoryId IS NULL OR b.category.id = :categoryId)
        """)
    Page<Book> findWithFilters(
            @Param("title") String title,
            @Param("author") String author,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}
