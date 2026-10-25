package com.servitech.authservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.servitech.authservice.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
    List<Cliente> findByDeletedAtIsNull();

    List<Cliente> findByUpdatedAtGreaterThan(Instant since);

    Optional<Cliente> findByUsuarioId(Long usuarioId);
}