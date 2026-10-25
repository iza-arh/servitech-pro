package com.servitech.authservice.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.servitech.authservice.model.OrdenTrabajo;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, String> {
    List<OrdenTrabajo> findByDeletedAtIsNull();

    List<OrdenTrabajo> findByUpdatedAtGreaterThan(Instant since);
}