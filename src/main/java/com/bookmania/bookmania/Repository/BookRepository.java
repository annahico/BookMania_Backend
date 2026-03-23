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
    SELECT DISTINCT b FROM Book b
    LEFT JOIN b.categories c
    WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%'))
           OR LOWER(b.author) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
    AND (:categoryId IS NULL OR c.id = :categoryId)
    """)
    Page<Book> findWithFilters(
            @Param("title") String title,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}
