package com.ims.repository;

import com.ims.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByCode(String code);

    @Query("SELECT s FROM Supplier s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Supplier> searchByNameOrContact(@Param("search") String search, Pageable pageable);

    List<Supplier> findByIsActiveTrue();

    @Query("SELECT COUNT(s) FROM Supplier s WHERE s.isActive = true")
    Long countActive();

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
