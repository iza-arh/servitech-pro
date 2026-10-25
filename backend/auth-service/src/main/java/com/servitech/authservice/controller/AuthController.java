package com.servitech.authservice.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.servitech.authservice.dto.LoginRequest;
import com.servitech.authservice.dto.LoginResponse;
import com.servitech.authservice.dto.RegisterRequest;
import com.servitech.authservice.model.Cliente;
import com.servitech.authservice.model.Rol;
import com.servitech.authservice.model.Usuario;
import com.servitech.authservice.repository.ClienteRepository;
import com.servitech.authservice.repository.UsuarioRepository;
import com.servitech.authservice.security.JwtUtil;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioRepository usuarioRepository,ClienteRepository clienteRepository,
            PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername()).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }

        String token = jwtUtil.generarToken(usuario.getUsername(), usuario.getRol().name());
        return ResponseEntity.ok(new LoginResponse(token, usuario.getRol().name()));
    }

    @PostMapping("/register")
@Transactional // si algo falla, no queda la cuenta sin su cliente
public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    if (request.getUsername() == null || request.getUsername().isBlank()
            || request.getPassword() == null || request.getPassword().length() < 8
            || request.getNombre() == null || request.getNombre().isBlank()) {
        return ResponseEntity.badRequest().body("Datos inválidos");
    }
    if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
        return ResponseEntity.status(409).body("El usuario ya existe");
    }

    Usuario usuario = new Usuario();
    usuario.setUsername(request.getUsername());
    usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    usuario.setRol(Rol.CLIENTE); // siempre CLIENTE, nunca se acepta del request
    usuario = usuarioRepository.save(usuario); // para obtener el id generado

    // Cada cuenta de cliente crea su registro en el catálogo, vinculado 1 a 1
    Cliente cliente = new Cliente();
    cliente.setId(UUID.randomUUID().toString());
    cliente.setNombre(request.getNombre().trim());
    cliente.setUsuarioId(usuario.getId());
    cliente.setUpdatedAt(Instant.now());
    clienteRepository.save(cliente);

    return ResponseEntity.status(201).build();
}
}