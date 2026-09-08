package com.bookmania.bookmania.Repository;

import com.bookmania.bookmania.Entity.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    Optional<Book> findByIsbn(String isbn);

    // Row-level lock held for the rest of the transaction: without it, two
    // concurrent loan/reservation requests for a book's last copy can both
    // read availableCopies > 0 before either commits its decrement, and both
    // succeed — over-lending the same copy. Only used on the read-then-write
    // path (LoanService/ReservationService.create); plain findById is fine
    // everywhere else (browsing, display) since it doesn't need to block.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

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
