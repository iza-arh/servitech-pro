package com.iza.servitech_pro.data.repository;

import android.content.Context;

import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.FechaUtil;
import com.iza.servitech_pro.data.local.entity.CatalogoEntity;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.data.local.entity.OrdenEntity;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.local.entity.SyncQueueItem;

import java.util.UUID;

public class LocalStore {

    public static final String CLIENTE = "CLIENTE";
    public static final String CATALOGO = "CATALOGO";
    public static final String ORDEN = "ORDEN";
    public static final String REGISTRO = "REGISTRO";

    private final AppDatabase db;

    public LocalStore(Context context) {
        this.db = AppDatabase.getInstance(context);
    }

    public static String nuevoId() {
        return UUID.randomUUID().toString();
    }

    // Deja una sola entrada pendiente por fila: el cambio nuevo reemplaza al anterior
    private void encolar(String entidad, String id, String operacion) {
        db.syncQueueDao().eliminarPorEntidad(entidad, id);
        SyncQueueItem it = new SyncQueueItem();
        it.entidad = entidad;
        it.entidadId = id;
        it.operacion = operacion;
        it.estado = "PENDIENTE";
        it.createdAt = System.currentTimeMillis();
        db.syncQueueDao().insert(it);
    }

    // ---------- Clientes ----------
    public void guardarCliente(ClienteEntity c) {
        db.runInTransaction(() -> {
            c.isSynced = false;
            c.localTs = System.currentTimeMillis();
            db.clienteDao().upsert(c);
            encolar(CLIENTE, c.id, "UPSERT");
        });
    }

    // ---------- Catálogo ----------
    public void guardarCatalogo(CatalogoEntity c) {
        db.runInTransaction(() -> {
            c.isSynced = false;
            c.localTs = System.currentTimeMillis();
            db.catalogoDao().upsert(c);
            encolar(CATALOGO, c.id, "UPSERT");
        });
    }

    public void borrarCatalogo(String id) {
        db.runInTransaction(() -> {
            CatalogoEntity c = db.catalogoDao().getById(id);
            if (c == null) {
                return;
            }
            c.deletedAt = FechaUtil.ahora();
            c.isSynced = false;
            c.localTs = System.currentTimeMillis();
            db.catalogoDao().upsert(c);
            encolar(CATALOGO, id, "DELETE");
        });
    }

    // ---------- Órdenes ----------
    public void guardarOrden(OrdenEntity o) {
        db.runInTransaction(() -> {
            o.isSynced = false;
            o.localTs = System.currentTimeMillis();
            db.ordenDao().upsert(o);
            encolar(ORDEN, o.id, "UPSERT");
        });
    }

    public void borrarOrden(String id) {
        db.runInTransaction(() -> {
            OrdenEntity o = db.ordenDao().getById(id);
            if (o == null) {
                return;
            }
            o.deletedAt = FechaUtil.ahora();
            o.isSynced = false;
            o.localTs = System.currentTimeMillis();
            db.ordenDao().upsert(o);
            encolar(ORDEN, id, "DELETE");
        });
    }

    // ---------- Registros (bitácora, repuesto usado, foto, firma) ----------
    // Devuelve null si se guardó, o el mensaje de error
    public String guardarRegistro(RegistroEntity r) {
        return db.runInTransaction(() -> {
            if ("FIRMA".equals(r.tipo)) {
                RegistroEntity firma = db.registroDao().getFirmaActiva(r.ordenId);
                if (firma != null && !firma.id.equals(r.id)) {
                    return "La orden ya tiene firma";
                }
            }
            r.isSynced = false;
            r.localTs = System.currentTimeMillis();
            db.registroDao().upsert(r);
            encolar(REGISTRO, r.id, "UPSERT");
            return null;
        });
    }

    public void borrarRegistro(String id) {
        db.runInTransaction(() -> {
            RegistroEntity r = db.registroDao().getById(id);
            if (r == null) {
                return;
            }
            r.deletedAt = FechaUtil.ahora();
            r.isSynced = false;
            r.localTs = System.currentTimeMillis();
            db.registroDao().upsert(r);
            encolar(REGISTRO, id, "DELETE");
        });
    }
}