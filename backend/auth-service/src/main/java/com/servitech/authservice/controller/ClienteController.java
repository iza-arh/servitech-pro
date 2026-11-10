package com.servitech.authservice.controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.servitech.authservice.dto.ClienteRequest;
import com.servitech.authservice.model.Cliente;
import com.servitech.authservice.model.Usuario;
import com.servitech.authservice.repository.ClienteRepository;
import com.servitech.authservice.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository repo;
    private final UsuarioRepository usuarioRepo;

    public ClienteController(ClienteRepository repo, UsuarioRepository usuarioRepo) {
        this.repo = repo;
        this.usuarioRepo = usuarioRepo;
    }

    // Sin "since": los activos. Con "since": todo lo cambiado después de esa fecha.
    // Un usuario con rol CLIENTE solo recibe su propio registro.
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String since, Authentication auth) {
        List<Cliente> base;
        if (since == null) {
            base = repo.findByDeletedAtIsNull();
        } else {
            try {
                base = repo.findByUpdatedAtGreaterThan(Instant.parse(since));
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("Fecha inválida");
            }
        }

        boolean esCliente = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        if (esCliente) {
            Usuario u = usuarioRepo.findByUsername(auth.getName()).orElse(null);
            final String miId = (u == null) ? null
                    : repo.findByUsuarioId(u.getId()).map(Cliente::getId).orElse(null);
            base = base.stream().filter(c -> c.getId().equals(miId)).toList();
        }
        return ResponseEntity.ok(base);
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