package com.servitech.authservice.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.servitech.authservice.model.CatalogoItem;

public interface CatalogoItemRepository extends JpaRepository<CatalogoItem, String> {
    List<CatalogoItem> findByDeletedAtIsNull();

    List<CatalogoItem> findByUpdatedAtGreaterThan(Instant since);
}