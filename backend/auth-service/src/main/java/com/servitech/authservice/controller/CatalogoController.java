package com.servitech.authservice.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.servitech.authservice.dto.CatalogoRequest;
import com.servitech.authservice.model.CatalogoItem;
import com.servitech.authservice.model.TipoCatalogo;
import com.servitech.authservice.repository.CatalogoItemRepository;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoItemRepository repo;

    public CatalogoController(CatalogoItemRepository repo) {
        this.repo = repo;
    }

    // Sin "since": los activos. Con "since": todo lo cambiado después (incluye
    // borrados)
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String since) {
        if (since == null) {
            return ResponseEntity.ok(repo.findByDeletedAtIsNull());
        }
        try {
            return ResponseEntity.ok(repo.findByUpdatedAtGreaterThan(Instant.parse(since)));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Fecha inválida");
        }
    }

    // Crea o actualiza (el id UUID lo genera el dispositivo). Solo ADMIN (ver
    // SecurityConfig)
    @PutMapping("/{id}")
    public ResponseEntity<?> guardar(@PathVariable String id, @RequestBody CatalogoRequest req) {
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Id inválido");
        }
        if (req.nombre == null || req.nombre.isBlank()) {
            return ResponseEntity.badRequest().body("El nombre es obligatorio");
        }
        TipoCatalogo tipo;
        try {
            tipo = TipoCatalogo.valueOf(req.tipo);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body("Tipo inválido");
        }
        if (req.precio != null && req.precio.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().body("El precio no puede ser negativo");
        }

        CatalogoItem item = repo.findById(id).orElse(null);
        if (item == null) {
            item = new CatalogoItem();
            item.id = id;
        } else if (item.deletedAt != null) {
            return ResponseEntity.status(409).body("El elemento fue eliminado");
        }

        item.tipo = tipo;
        item.nombre = req.nombre.trim();
        item.descripcion = req.descripcion;
        item.codigo = req.codigo;
        item.precio = req.precio;
        item.updatedAt = Instant.now();
        return ResponseEntity.ok(repo.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable String id) {
        repo.findById(id).ifPresent(i -> {
            if (i.deletedAt == null) {
                i.deletedAt = Instant.now();
                i.updatedAt = Instant.now();
                repo.save(i);
            }
        });
        return ResponseEntity.noContent().build();
    }
}