package com.ims.repository;

import com.ims.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findByCode(String code);

    Optional<Currency> findByIsBaseTrue();

    List<Currency> findByIsActiveTrue();

    boolean existsByCode(String code);
}