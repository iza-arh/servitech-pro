package com.iza.servitech_pro.ui.catalogo;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.dao.CatalogoDao;
import com.iza.servitech_pro.data.local.dao.ClienteDao;
import com.iza.servitech_pro.data.local.entity.CatalogoEntity;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.data.local.entity.SyncQueueItem;
import com.iza.servitech_pro.data.sync.SyncManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CatalogoViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final ClienteDao clienteDao;
    private final CatalogoDao catalogoDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Texto del buscador (la Activity lo escribe, los Fragments lo observan)
    private final MutableLiveData<String> textoBusqueda = new MutableLiveData<>("");

    public final LiveData<List<ClienteEntity>> clientes;

    public CatalogoViewModel(@NonNull Application app) {
        super(app);
        db = AppDatabase.getInstance(app);
        clienteDao = db.clienteDao();
        catalogoDao = db.catalogoDao();

        clientes = Transformations.switchMap(textoBusqueda, texto ->
                texto == null || texto.trim().isEmpty()
                        ? clienteDao.observarActivos()
                        : clienteDao.buscarActivos(texto.trim()));
    }

    public void setTextoBusqueda(String texto) {
        textoBusqueda.setValue(texto);
    }

    // ---------- Clientes ----------

    public void guardarCliente(ClienteEntity c) {
        executor.execute(() -> {
            if (c.id == null || c.id.isEmpty()) c.id = UUID.randomUUID().toString();
            c.isSynced = false;
            c.localTs = System.currentTimeMillis();

            db.runInTransaction(() -> {
                clienteDao.upsert(c);
                encolar("CLIENTE", c.id, "UPSERT");
            });
            SyncManager.getInstance(getApplication()).sincronizar();
        });
    }

    public void eliminarCliente(ClienteEntity c) {
        executor.execute(() -> {
            // Se lee la fila actual para no pisarla con una copia vieja
            ClienteEntity actual = clienteDao.getById(c.id);
            if (actual == null) return;

            actual.deletedAt = ahoraIso();
            actual.isSynced = false;
            actual.localTs = System.currentTimeMillis();

            db.runInTransaction(() -> {
                clienteDao.upsert(actual);
                encolar("CLIENTE", actual.id, "DELETE");
            });
            SyncManager.getInstance(getApplication()).sincronizar();
        });
    }

    // ---------- Catálogo (servicios, equipos, repuestos) ----------

    public LiveData<List<CatalogoEntity>> observarCatalogo(String tipo) {
        return Transformations.switchMap(textoBusqueda, texto ->
                texto == null || texto.trim().isEmpty()
                        ? catalogoDao.observarPorTipo(tipo)
                        : catalogoDao.buscarPorTipo(tipo, texto.trim()));
    }

    public void guardarItem(CatalogoEntity c) {
        executor.execute(() -> {
            if (c.id == null || c.id.isEmpty()) c.id = UUID.randomUUID().toString();
            c.isSynced = false;
            c.localTs = System.currentTimeMillis();

            db.runInTransaction(() -> {
                catalogoDao.upsert(c);
                encolar("CATALOGO", c.id, "UPSERT");
            });
            SyncManager.getInstance(getApplication()).sincronizar();
        });
    }

    public void eliminarItem(CatalogoEntity c) {
        executor.execute(() -> {
            CatalogoEntity actual = catalogoDao.getById(c.id);
            if (actual == null) return;

            actual.deletedAt = ahoraIso();
            actual.isSynced = false;
            actual.localTs = System.currentTimeMillis();

            db.runInTransaction(() -> {
                catalogoDao.upsert(actual);
                encolar("CATALOGO", actual.id, "DELETE");
            });
            SyncManager.getInstance(getApplication()).sincronizar();
        });
    }

    // ---------- Cola de sincronización ----------

    // Un cambio nuevo reemplaza al anterior de la misma fila
    private void encolar(String entidad, String id, String operacion) {
        db.syncQueueDao().eliminarPorEntidad(entidad, id);
        SyncQueueItem q = new SyncQueueItem();
        q.entidad = entidad;
        q.operacion = operacion;
        q.entidadId = id;
        q.estado = "PENDIENTE";
        db.syncQueueDao().insert(q);
    }

    private String ahoraIso() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
