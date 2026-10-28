package com.servitech.authservice.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.servitech.authservice.model.RegistroOrden;
import com.servitech.authservice.model.TipoRegistro;

public interface RegistroOrdenRepository extends JpaRepository<RegistroOrden, String> {
    List<RegistroOrden> findByDeletedAtIsNull();

    List<RegistroOrden> findByUpdatedAtGreaterThan(Instant since);

    List<RegistroOrden> findByOrdenIdAndTipoAndDeletedAtIsNull(String ordenId, TipoRegistro tipo);
}