package com.servitech.authservice.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.servitech.authservice.dto.SyncItem;
import com.servitech.authservice.dto.SyncRequest;
import com.servitech.authservice.dto.SyncResponse;
import com.servitech.authservice.dto.SyncResult;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private static final int MAX_ITEMS = 200;

    private final ClienteController clientes;
    private final CatalogoController catalogo;
    private final OrdenTrabajoController ordenes;
    private final RegistroOrdenController registros;

    public SyncController(ClienteController clientes, CatalogoController catalogo,
            OrdenTrabajoController ordenes, RegistroOrdenController registros) {
        this.clientes = clientes;
        this.catalogo = catalogo;
        this.ordenes = ordenes;
        this.registros = registros;
    }

    private static int prioridad(String entidad) {
        if (entidad == null)
            return 99;
        switch (entidad) {
            case "CLIENTE":
                return 1;
            case "CATALOGO":
                return 2;
            case "ORDEN":
                return 3;
            case "REGISTRO":
                return 4;
            default:
                return 99;
        }
    }

    private static boolean tieneRol(Authentication auth, String rol) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(rol));
    }

    @PostMapping
    public ResponseEntity<?> sincronizar(@RequestBody SyncRequest req, Authentication auth) {
        if (req == null || req.items == null) {
            return ResponseEntity.badRequest().body("Faltan items");
        }
        if (req.items.size() > MAX_ITEMS) {
            return ResponseEntity.badRequest().body("Máximo " + MAX_ITEMS + " items por solicitud");
        }
        boolean admin = tieneRol(auth, "ROLE_ADMIN");
        if (!admin && !tieneRol(auth, "ROLE_TECNICO")) {
            return ResponseEntity.status(403).body("Rol no permitido");
        }

        List<SyncItem> upserts = new ArrayList<>();
        List<SyncItem> deletes = new ArrayList<>();
        List<SyncResult> resultados = new ArrayList<>();

        for (SyncItem it : req.items) {
            if (it == null) {
                continue;
            }
            if ("UPSERT".equals(it.operacion)) {
                upserts.add(it);
            } else if ("DELETE".equals(it.operacion)) {
                deletes.add(it);
            } else {
                resultados.add(resultado(it, 400, "Operación inválida"));
            }
        }

        // Upserts: clientes -> catálogo -> órdenes -> registros. Deletes: al revés.
        upserts.sort(Comparator.comparingInt(i -> prioridad(i.entidad)));
        deletes.sort(Comparator.comparingInt((SyncItem i) -> prioridad(i.entidad)).reversed());

        for (SyncItem it : upserts) {
            resultados.add(procesar(it, admin, auth));
        }
        for (SyncItem it : deletes) {
            resultados.add(procesar(it, admin, auth));
        }

        SyncResponse resp = new SyncResponse();
        resp.resultados = resultados;
        for (SyncResult r : resultados) {
            if (r.ok) {
                resp.aceptados++;
            } else {
                resp.rechazados++;
            }
        }
        return ResponseEntity.ok(resp);
    }

    private SyncResult procesar(SyncItem it, boolean admin, Authentication auth) {
        try {
            ResponseEntity<?> resp = ejecutar(it, admin, auth);
            Object body = resp.getBody();
            return resultado(it, resp.getStatusCode().value(),
                    resp.getStatusCode().is2xxSuccessful() ? null
                            : (body != null ? body.toString() : "Rechazado"));
        } catch (ResponseStatusException e) {
            return resultado(it, e.getStatusCode().value(), e.getReason());
        } catch (Exception e) {
            return resultado(it, 500, "Error interno");
        }
    }

    private ResponseEntity<?> ejecutar(SyncItem it, boolean admin, Authentication auth) {
        if (it.id == null || it.entidad == null) {
            return ResponseEntity.badRequest().body("Item incompleto");
        }
        boolean borrar = "DELETE".equals(it.operacion);

        switch (it.entidad) {
            case "CLIENTE":
                if (!admin)
                    return ResponseEntity.status(403).body("Solo administrador");
                if (borrar || it.cliente == null)
                    return ResponseEntity.badRequest().body("Operación no soportada");
                return clientes.editar(it.id, it.cliente);

            case "CATALOGO":
                if (!admin)
                    return ResponseEntity.status(403).body("Solo administrador");
                if (borrar)
                    return catalogo.borrar(it.id);
                if (it.catalogo == null)
                    return ResponseEntity.badRequest().body("Falta catalogo");
                return catalogo.guardar(it.id, it.catalogo);

            case "ORDEN":
                if (borrar) {
                    if (!admin)
                        return ResponseEntity.status(403).body("Solo administrador");
                    return ordenes.borrar(it.id);
                }
                if (it.orden == null)
                    return ResponseEntity.badRequest().body("Falta orden");
                return ordenes.guardar(it.id, it.orden, auth);

            case "REGISTRO":
                if (borrar)
                    return registros.borrar(it.id, auth);
                if (it.registro == null)
                    return ResponseEntity.badRequest().body("Falta registro");
                return registros.guardar(it.id, it.registro, auth);

            default:
                return ResponseEntity.badRequest().body("Entidad inválida");
        }
    }

    private SyncResult resultado(SyncItem it, int status, String mensaje) {
        SyncResult r = new SyncResult();
        r.id = it.id;
        r.entidad = it.entidad;
        r.operacion = it.operacion;
        r.status = status;
        r.ok = status >= 200 && status < 300;
        r.mensaje = mensaje;
        return r;
    }
}