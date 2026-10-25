package com.servitech.authservice.controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.servitech.authservice.dto.OrdenTrabajoRequest;
import com.servitech.authservice.model.Cliente;
import com.servitech.authservice.model.EstadoOrden;
import com.servitech.authservice.model.OrdenTrabajo;
import com.servitech.authservice.model.Rol;
import com.servitech.authservice.model.Usuario;
import com.servitech.authservice.repository.ClienteRepository;
import com.servitech.authservice.repository.OrdenTrabajoRepository;
import com.servitech.authservice.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenTrabajoController {

    private final OrdenTrabajoRepository repo;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public OrdenTrabajoController(OrdenTrabajoRepository repo,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository) {
        this.repo = repo;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario usuarioActual(Authentication auth) {
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    // Única regla de visibilidad: todo el acceso por rol pasa por aquí
    private boolean puedeVer(OrdenTrabajo o, Usuario u, String miClienteId) {
        switch (u.getRol()) {
            case ADMIN:
                return true;
            case TECNICO:
                return u.getId().equals(o.getTecnicoId());
            case CLIENTE:
                return miClienteId != null && miClienteId.equals(o.getClienteId());
            default:
                return false;
        }
    }

    // Sin "since": las no borradas. Con "since": todo lo cambiado después,
    // incluidas las borradas, para que el dispositivo las elimine
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String since,
            Authentication auth) {
        Usuario u = usuarioActual(auth);

        List<OrdenTrabajo> base;
        if (since == null) {
            base = repo.findByDeletedAtIsNull();
        } else {
            try {
                base = repo.findByUpdatedAtGreaterThan(Instant.parse(since));
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("Fecha inválida");
            }
        }

        // El cliente se deduce de la cuenta del token, nunca de la petición
        final String miClienteId = (u.getRol() == Rol.CLIENTE)
                ? clienteRepository.findByUsuarioId(u.getId()).map(Cliente::getId).orElse(null)
                : null;

        return ResponseEntity.ok(
                base.stream().filter(o -> puedeVer(o, u, miClienteId)).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> guardar(@PathVariable String id,
            @RequestBody OrdenTrabajoRequest req,
            Authentication auth) {
        Usuario u = usuarioActual(auth);

        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Id inválido");
        }
        if (req.getPlaca() == null || req.getPlaca().isBlank()
                || req.getDescripcion() == null || req.getDescripcion().isBlank()
                || req.getClienteId() == null) {
            return ResponseEntity.badRequest().body("Faltan datos obligatorios");
        }

        EstadoOrden estado;
        try {
            estado = EstadoOrden.valueOf(req.getEstado());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body("Estado inválido");
        }

        Cliente cliente = clienteRepository.findById(req.getClienteId()).orElse(null);
        if (cliente == null || cliente.getDeletedAt() != null) {
            return ResponseEntity.badRequest().body("Cliente inválido");
        }

        OrdenTrabajo o = repo.findById(id).orElse(null);
        if (o == null) {
            o = new OrdenTrabajo();
            o.setId(id);
        } else if (o.getDeletedAt() != null) {
            return ResponseEntity.status(409).body("La orden fue eliminada");
        } else if (u.getRol() == Rol.TECNICO && !u.getId().equals(o.getTecnicoId())) {
            return ResponseEntity.status(403).body("La orden no está asignada a ti");
        }

        if (u.getRol() == Rol.TECNICO) {
            o.setTecnicoId(u.getId()); // el técnico siempre queda como responsable
        } else if (req.getTecnicoId() != null) {
            Usuario t = usuarioRepository.findById(req.getTecnicoId()).orElse(null);
            if (t == null || t.getRol() != Rol.TECNICO) {
                return ResponseEntity.badRequest().body("Técnico inválido");
            }
            o.setTecnicoId(t.getId());
        }

        o.setClienteId(req.getClienteId());
        o.setPlaca(req.getPlaca().trim());
        o.setMarca(req.getMarca());
        o.setModelo(req.getModelo());
        o.setAnio(req.getAnio());
        o.setDescripcion(req.getDescripcion().trim());
        o.setEstado(estado);
        o.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(repo.save(o));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable String id) {
        repo.findById(id).ifPresent(o -> {
            if (o.getDeletedAt() == null) {
                o.setDeletedAt(Instant.now());
                o.setUpdatedAt(Instant.now());
                repo.save(o);
            }
        });
        return ResponseEntity.noContent().build();
    }
}