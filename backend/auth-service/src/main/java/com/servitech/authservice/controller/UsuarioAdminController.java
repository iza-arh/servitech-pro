package com.servitech.authservice.controller;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.servitech.authservice.model.Rol;
import com.servitech.authservice.model.Usuario;
import com.servitech.authservice.repository.UsuarioRepository;

// Solo ADMIN (la regla /api/admin/** ya está en SecurityConfig)
@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioAdminController {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public UsuarioAdminController(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public static class UsuarioDto {
        public Long id;
        public String username;
        public String nombre;
        public String rol;
    }

    public static class CrearRequest {
        public String username;
        public String password;
        public String nombre;
        public String rol;
    }

    public static class PasswordRequest {
        public String password;
    }

    private UsuarioDto aDto(Usuario u) {
        UsuarioDto d = new UsuarioDto();
        d.id = u.getId();
        d.username = u.getUsername();
        d.nombre = u.getNombre();
        d.rol = String.valueOf(u.getRol());
        return d;
    }

    // Nunca se devuelve el hash de la contraseña
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String rol) {
        Rol filtro = null;
        if (rol != null && !rol.isBlank()) {
            try {
                filtro = Rol.valueOf(rol);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Rol inválido");
            }
        }
        final Rol f = filtro;
        List<UsuarioDto> lista = repo.findAll().stream()
                .filter(u -> f == null || u.getRol() == f)
                .sorted(Comparator.comparing(u -> u.getUsername().toLowerCase()))
                .map(this::aDto)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearRequest req) {
        if (req == null || req.username == null || !USERNAME.matcher(req.username.trim()).matches()) {
            return ResponseEntity.badRequest()
                    .body("Usuario inválido: 3 a 30 caracteres, sin espacios (letras, números, punto o guion)");
        }
        if (req.password == null || req.password.length() < 8 || req.password.length() > 72) {
            return ResponseEntity.badRequest().body("La contraseña debe tener entre 8 y 72 caracteres");
        }
        if (req.nombre == null || req.nombre.trim().length() < 2 || req.nombre.trim().length() > 100) {
            return ResponseEntity.badRequest().body("El nombre debe tener entre 2 y 100 caracteres");
        }
        Rol rol;
        try {
            rol = Rol.valueOf(req.rol);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body("Rol inválido");
        }
        if (rol != Rol.TECNICO && rol != Rol.ADMIN) {
            return ResponseEntity.badRequest().body("Solo se pueden crear técnicos o administradores");
        }
        String username = req.username.trim();
        if (repo.findByUsername(username).isPresent()) {
            return ResponseEntity.status(409).body("El usuario ya existe");
        }

        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(req.password));
        u.setNombre(req.nombre.trim());
        u.setRol(rol);
        return ResponseEntity.status(201).body(aDto(repo.save(u)));
    }

    // El administrador restablece la contraseña de otro usuario
    @PutMapping("/{id}/password")
    public ResponseEntity<?> cambiarPassword(@PathVariable Long id, @RequestBody PasswordRequest req) {
        Usuario u = repo.findById(id).orElse(null);
        if (u == null) {
            return ResponseEntity.notFound().build();
        }
        if (req == null || req.password == null || req.password.length() < 8 || req.password.length() > 72) {
            return ResponseEntity.badRequest().body("La contraseña debe tener entre 8 y 72 caracteres");
        }
        u.setPasswordHash(encoder.encode(req.password));
        repo.save(u);
        return ResponseEntity.noContent().build();
    }
}