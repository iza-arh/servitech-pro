package com.servitech.authservice.controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.servitech.authservice.dto.ClienteRequest;
import com.servitech.authservice.model.Cliente;
import com.servitech.authservice.repository.ClienteRepository;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository repo;

    public ClienteController(ClienteRepository repo) {
        this.repo = repo;
    }

    // Sin "since": los activos. Con "since": todo lo cambiado después de esa fecha
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

    // Los clientes se crean al registrarse; aquí solo se editan sus datos de
    // contacto
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable String id, @RequestBody ClienteRequest req) {
        if (req.getNombre() == null || req.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body("El nombre es obligatorio");
        }
        Cliente c = repo.findById(id).orElse(null);
        if (c == null) {
            return ResponseEntity.notFound().build();
        }
        c.setNombre(req.getNombre().trim());
        c.setTelefono(req.getTelefono());
        c.setCorreo(req.getCorreo());
        c.setDireccion(req.getDireccion());
        c.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(repo.save(c));
    }
}