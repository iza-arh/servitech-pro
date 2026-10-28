package com.servitech.authservice.controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.servitech.authservice.dto.RegistroRequest;
import com.servitech.authservice.model.CatalogoItem;
import com.servitech.authservice.model.Cliente;
import com.servitech.authservice.model.OrdenTrabajo;
import com.servitech.authservice.model.RegistroOrden;
import com.servitech.authservice.model.TipoCatalogo;
import com.servitech.authservice.model.TipoRegistro;
import com.servitech.authservice.model.Usuario;
import com.servitech.authservice.repository.CatalogoItemRepository;
import com.servitech.authservice.repository.ClienteRepository;
import com.servitech.authservice.repository.OrdenTrabajoRepository;
import com.servitech.authservice.repository.RegistroOrdenRepository;
import com.servitech.authservice.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/registros")
public class RegistroOrdenController {

    private static final int MAX_DATOS = 4_000_000; // ~3 MB de imagen en base64

    private final RegistroOrdenRepository repo;
    private final OrdenTrabajoRepository ordenRepo;
    private final ClienteRepository clienteRepo;
    private final UsuarioRepository usuarioRepo;
    private final CatalogoItemRepository catalogoRepo;

    public RegistroOrdenController(RegistroOrdenRepository repo, OrdenTrabajoRepository ordenRepo,
            ClienteRepository clienteRepo, UsuarioRepository usuarioRepo,
            CatalogoItemRepository catalogoRepo) {
        this.repo = repo;
        this.ordenRepo = ordenRepo;
        this.clienteRepo = clienteRepo;
        this.usuarioRepo = usuarioRepo;
        this.catalogoRepo = catalogoRepo;
    }

    private Usuario usuarioActual(Authentication auth) {
        return usuarioRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    // Misma regla de visibilidad que las órdenes
    private boolean puedeVer(OrdenTrabajo o, Usuario u) {
        switch (u.getRol()) {
            case ADMIN:
                return true;
            case TECNICO:
                return u.getId().equals(o.getTecnicoId());
            case CLIENTE:
                return clienteRepo.findByUsuarioId(u.getId())
                        .map(Cliente::getId)
                        .map(id -> id.equals(o.getClienteId()))
                        .orElse(false);
            default:
                return false;
        }
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String since, Authentication auth) {
        Usuario u = usuarioActual(auth);

        List<RegistroOrden> base;
        if (since == null) {
            base = repo.findByDeletedAtIsNull();
        } else {
            try {
                base = repo.findByUpdatedAtGreaterThan(Instant.parse(since));
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("Fecha inválida");
            }
        }

        Map<String, Boolean> visibles = new HashMap<>(); // evita consultar la misma orden varias veces
        return ResponseEntity.ok(base.stream().filter(r -> visibles.computeIfAbsent(r.ordenId,
                oid -> ordenRepo.findById(oid).map(o -> puedeVer(o, u)).orElse(false))).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> guardar(@PathVariable String id, @RequestBody RegistroRequest req,
            Authentication auth) {
        Usuario u = usuarioActual(auth);

        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Id inválido");
        }
        if (req.ordenId == null) {
            return ResponseEntity.badRequest().body("Falta la orden");
        }
        TipoRegistro tipo;
        try {
            tipo = TipoRegistro.valueOf(req.tipo);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body("Tipo inválido");
        }

        OrdenTrabajo orden = ordenRepo.findById(req.ordenId).orElse(null);
        if (orden == null || orden.getDeletedAt() != null) {
            return ResponseEntity.badRequest().body("Orden inválida");
        }
        if (!puedeVer(orden, u)) {
            return ResponseEntity.status(403).body("La orden no está asignada a ti");
        }

        // Validaciones por tipo
        switch (tipo) {
            case BITACORA:
                if (req.texto == null || req.texto.isBlank() || req.texto.length() > 2000) {
                    return ResponseEntity.badRequest().body("Texto obligatorio (máx. 2000 caracteres)");
                }
                break;
            case REPUESTO:
                if (req.cantidad == null || req.cantidad <= 0) {
                    return ResponseEntity.badRequest().body("La cantidad debe ser mayor a 0");
                }
                CatalogoItem item = req.catalogoItemId == null ? null
                        : catalogoRepo.findById(req.catalogoItemId).orElse(null);
                if (item == null || item.deletedAt != null || item.tipo != TipoCatalogo.REPUESTO) {
                    return ResponseEntity.badRequest().body("Repuesto inválido");
                }
                break;
            case FOTO:
            case FIRMA:
                if (req.datos == null || req.datos.isBlank() || req.datos.length() > MAX_DATOS) {
                    return ResponseEntity.badRequest().body("Imagen obligatoria (máx. ~3 MB)");
                }
                break;
        }

        RegistroOrden r = repo.findById(id).orElse(null);
        if (r == null) {
            r = new RegistroOrden();
            r.id = id;
        } else if (r.deletedAt != null) {
            return ResponseEntity.status(409).body("El registro fue eliminado");
        } else if (!r.ordenId.equals(req.ordenId) || r.tipo != tipo) {
            return ResponseEntity.badRequest().body("No se puede cambiar la orden ni el tipo");
        }

        // Una sola firma activa por orden
        if (tipo == TipoRegistro.FIRMA) {
            final String propioId = id;
            boolean otra = repo.findByOrdenIdAndTipoAndDeletedAtIsNull(req.ordenId, TipoRegistro.FIRMA)
                    .stream().anyMatch(f -> !f.id.equals(propioId));
            if (otra) {
                return ResponseEntity.status(409).body("La orden ya tiene firma");
            }
        }

        r.ordenId = req.ordenId;
        r.tipo = tipo;
        r.texto = req.texto;
        r.catalogoItemId = req.catalogoItemId;
        r.cantidad = req.cantidad;
        r.datos = req.datos;
        r.updatedAt = Instant.now();
        return ResponseEntity.ok(repo.save(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable String id, Authentication auth) {
        Usuario u = usuarioActual(auth);
        RegistroOrden r = repo.findById(id).orElse(null);
        if (r == null || r.deletedAt != null) {
            return ResponseEntity.noContent().build(); // idempotente
        }
        OrdenTrabajo orden = ordenRepo.findById(r.ordenId).orElse(null);
        if (orden == null || !puedeVer(orden, u)) {
            return ResponseEntity.status(403).build();
        }
        r.deletedAt = Instant.now();
        r.updatedAt = Instant.now();
        repo.save(r);
        return ResponseEntity.noContent().build();
    }
}