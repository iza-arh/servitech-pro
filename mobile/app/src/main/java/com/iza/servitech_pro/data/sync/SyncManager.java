package com.iza.servitech_pro.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.FechaUtil;
import com.iza.servitech_pro.data.local.entity.CatalogoEntity;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.data.local.entity.OrdenEntity;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.local.entity.SyncQueueItem;
import com.iza.servitech_pro.data.remote.RetrofitClient;
import com.iza.servitech_pro.data.remote.api.SyncApiService;
import com.iza.servitech_pro.data.remote.dto.SyncDto;
import com.iza.servitech_pro.data.repository.SecurePrefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Response;

public class SyncManager {

    private static final int LOTE = 25;
    private static volatile SyncManager INSTANCE;

    public static SyncManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SyncManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SyncManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private final Context app;
    private final AppDatabase db;
    // Hilo propio: así una sincronización lenta no bloquea los guardados locales
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean ocupado = new AtomicBoolean(false);
    private final AtomicBoolean repetir = new AtomicBoolean(false);
    private final MutableLiveData<SyncEstado> estado = new MutableLiveData<>(SyncEstado.SIN_CONEXION);
    private final MutableLiveData<SyncResultado> ultimoResultado = new MutableLiveData<>();
    private volatile boolean hayRed = false;
    private boolean iniciado = false;

    // SOLO PRUEBAS: simula que no hay servidor
    public volatile boolean simularSinRed = false;

    private SyncManager(Context app) {
        this.app = app;
        this.db = AppDatabase.getInstance(app);
    }

    // ---------- API pública ----------

    public LiveData<SyncEstado> estado() {
        return estado;
    }

    public LiveData<SyncResultado> ultimoResultado() {
        return ultimoResultado;
    }

    // Registra el detector de red. Se puede llamar muchas veces (solo actúa la primera)
    public synchronized void iniciar() {
        if (iniciado) {
            return;
        }
        iniciado = true;
        ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        hayRed = hayInternet(cm);
        estado.postValue(hayRed ? SyncEstado.EN_LINEA : SyncEstado.SIN_CONEXION);
        try {
            cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    hayRed = true;
                    if (!ocupado.get()) {
                        estado.postValue(SyncEstado.EN_LINEA);
                    }
                    sincronizar(); // volvió la red: sincroniza solo
                }

                @Override
                public void onLost(Network network) {
                    hayRed = false;
                    if (!ocupado.get()) {
                        estado.postValue(SyncEstado.SIN_CONEXION);
                    }
                }
            });
        } catch (Exception e) {
            iniciado = false;
        }
    }

    // Pide una sincronización en segundo plano (si ya hay una, se repite al terminar)
    public void sincronizar() {
        if (!ocupado.compareAndSet(false, true)) {
            repetir.set(true);
            return;
        }
        executor.execute(() -> {
            try {
                correr();
            } finally {
                ocupado.set(false);
                if (repetir.getAndSet(false)) {
                    sincronizar();
                }
            }
        });
    }

    // Versión que espera el resultado (para pruebas). Debe llamarse fuera del hilo principal
    public SyncResultado sincronizarBloqueante() throws InterruptedException {
        long limite = System.currentTimeMillis() + 60_000;
        while (!ocupado.compareAndSet(false, true)) {
            if (System.currentTimeMillis() > limite) {
                return null;
            }
            Thread.sleep(200);
        }
        try {
            return correr();
        } finally {
            ocupado.set(false);
        }
    }

    // ---------- Motor ----------

    private SyncResultado correr() {
        estado.postValue(SyncEstado.SINCRONIZANDO);
        SyncResultado r = ejecutar();
        ultimoResultado.postValue(r);
        if (r.sesionExpirada) {
            estado.postValue(SyncEstado.SESION_EXPIRADA);
        } else if (r.error != null) {
            estado.postValue(hayRed ? SyncEstado.ERROR : SyncEstado.SIN_CONEXION);
        } else {
            estado.postValue(SyncEstado.EN_LINEA);
        }
        return r;
    }

    private SyncResultado ejecutar() {
        SyncResultado res = new SyncResultado();
        if (simularSinRed) {
            res.error = "Sin conexión (simulada)";
            return res;
        }
        String token = SecurePrefs.leerToken(app);
        if (token == null) {
            res.sesionExpirada = true;
            res.error = "Sin sesión";
            return res;
        }
        String auth = "Bearer " + token;
        SyncApiService api = RetrofitClient.getSyncApi();
        try {
            if (empujar(api, auth, res)) {
                recibir(api, auth, res);
            }
        } catch (IOException e) {
            res.error = "Sin conexión con el servidor";
        } catch (Exception e) {
            res.error = "Error: " + e.getMessage();
        }
        return res;
    }

    private static boolean hayInternet(ConnectivityManager cm) {
        Network n = cm.getActiveNetwork();
        if (n == null) {
            return false;
        }
        NetworkCapabilities nc = cm.getNetworkCapabilities(n);
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // ---------- Envío (push) ----------

    private static class Enviado {
        SyncQueueItem q;
        SyncDto.Item item;
        long localTs;
    }

    // false = abortar (sin red, sesión vencida o error del servidor)
    private boolean empujar(SyncApiService api, String auth, SyncResultado res) throws IOException {
        List<SyncQueueItem> pendientes = db.syncQueueDao().getPendientes();
        for (int i = 0; i < pendientes.size(); i += LOTE) {
            List<SyncQueueItem> lote = pendientes.subList(i, Math.min(i + LOTE, pendientes.size()));
            SyncDto.Request req = new SyncDto.Request();
            req.items = new ArrayList<>();
            Map<String, Enviado> porClave = new HashMap<>();

            for (SyncQueueItem q : lote) {
                Enviado e = preparar(q);
                if (e == null) {
                    db.syncQueueDao().eliminar(q.id); // la fila ya no existe
                    continue;
                }
                req.items.add(e.item);
                porClave.put(q.entidad + ":" + q.entidadId, e);
            }
            if (req.items.isEmpty()) {
                continue;
            }

            Response<SyncDto.Response> r = api.sync(auth, req).execute();
            if (r.code() == 401) {
                res.sesionExpirada = true;
                res.error = "Sesión expirada";
                return false;
            }
            if (!r.isSuccessful() || r.body() == null || r.body().resultados == null) {
                res.error = "El servidor respondió " + r.code();
                for (Enviado e : porClave.values()) {
                    db.syncQueueDao().registrarIntentoFallido(e.q.id, res.error);
                }
                return false;
            }

            for (SyncDto.Result x : r.body().resultados) {
                Enviado e = porClave.get(x.entidad + ":" + x.id);
                if (e == null) {
                    continue;
                }
                if (x.ok) {
                    marcarSincronizado(e.q.entidad, e.q.entidadId, e.localTs);
                    db.syncQueueDao().eliminar(e.q.id);
                    res.enviados++;
                } else if (x.status >= 500) {
                    db.syncQueueDao().registrarIntentoFallido(e.q.id, x.mensaje);
                } else {
                    db.syncQueueDao().marcarRechazado(e.q.id, x.mensaje);
                    res.rechazados++;
                }
            }
        }
        return true;
    }

    private Enviado preparar(SyncQueueItem q) {
        SyncDto.Item it = new SyncDto.Item();
        it.entidad = q.entidad;
        it.operacion = q.operacion;
        it.id = q.entidadId;
        boolean upsert = "UPSERT".equals(q.operacion);
        long ts;

        switch (q.entidad) {
            case "CLIENTE": {
                ClienteEntity c = db.clienteDao().getById(q.entidadId);
                if (c == null) return null;
                ts = c.localTs;
                if (upsert) {
                    SyncDto.ClienteReq r = new SyncDto.ClienteReq();
                    r.nombre = c.nombre;
                    r.telefono = c.telefono;
                    r.correo = c.correo;
                    r.direccion = c.direccion;
                    it.cliente = r;
                }
                break;
            }
            case "CATALOGO": {
                CatalogoEntity c = db.catalogoDao().getById(q.entidadId);
                if (c == null) return null;
                ts = c.localTs;
                if (upsert) {
                    SyncDto.CatalogoReq r = new SyncDto.CatalogoReq();
                    r.tipo = c.tipo;
                    r.nombre = c.nombre;
                    r.descripcion = c.descripcion;
                    r.codigo = c.codigo;
                    r.precio = c.precio;
                    it.catalogo = r;
                }
                break;
            }
            case "ORDEN": {
                OrdenEntity o = db.ordenDao().getById(q.entidadId);
                if (o == null) return null;
                ts = o.localTs;
                if (upsert) {
                    SyncDto.OrdenReq r = new SyncDto.OrdenReq();
                    r.clienteId = o.clienteId;
                    r.tecnicoId = o.tecnicoId;
                    r.tipoServicioId = o.tipoServicioId;
                    r.categoriaEquipoId = o.categoriaEquipoId;
                    r.placa = o.placa;
                    r.marca = o.marca;
                    r.modelo = o.modelo;
                    r.anio = o.anio;
                    r.descripcion = o.descripcion;
                    r.estado = o.estado;
                    it.orden = r;
                }
                break;
            }
            case "REGISTRO": {
                RegistroEntity g = db.registroDao().getById(q.entidadId);
                if (g == null) return null;
                ts = g.localTs;
                if (upsert) {
                    SyncDto.RegistroReq r = new SyncDto.RegistroReq();
                    r.ordenId = g.ordenId;
                    r.tipo = g.tipo;
                    r.texto = g.texto;
                    r.catalogoItemId = g.catalogoItemId;
                    r.cantidad = g.cantidad;
                    r.datos = g.datos;
                    it.registro = r;
                }
                break;
            }
            default:
                return null;
        }

        Enviado e = new Enviado();
        e.q = q;
        e.item = it;
        e.localTs = ts;
        return e;
    }

    // Solo marca si la fila no cambió mientras se enviaba
    private void marcarSincronizado(String entidad, String id, long localTs) {
        switch (entidad) {
            case "CLIENTE": db.clienteDao().marcarSincronizado(id, localTs); break;
            case "CATALOGO": db.catalogoDao().marcarSincronizado(id, localTs); break;
            case "ORDEN": db.ordenDao().marcarSincronizado(id, localTs); break;
            case "REGISTRO": db.registroDao().marcarSincronizado(id, localTs); break;
            default: break;
        }
    }

    // ---------- Descarga (pull) ----------

    private <T> List<T> pedir(Call<List<T>> call, SyncResultado res) throws IOException {
        Response<List<T>> r = call.execute();
        if (r.code() == 401) {
            res.sesionExpirada = true;
            res.error = "Sesión expirada";
            return null;
        }
        if (!r.isSuccessful()) {
            if (res.error == null) {
                res.error = "El servidor respondió " + r.code();
            }
            return null;
        }
        return r.body();
    }

    private void recibir(SyncApiService api, String auth, SyncResultado res) throws IOException {
        List<ClienteEntity> clientes = pedir(api.clientes(auth, db.clienteDao().maxUpdatedAt()), res);
        if (res.sesionExpirada) return;
        if (clientes != null) aplicarClientes(clientes, res);

        List<CatalogoEntity> catalogo = pedir(api.catalogo(auth, db.catalogoDao().maxUpdatedAt()), res);
        if (res.sesionExpirada) return;
        if (catalogo != null) aplicarCatalogo(catalogo, res);

        List<OrdenEntity> ordenes = pedir(api.ordenes(auth, db.ordenDao().maxUpdatedAt()), res);
        if (res.sesionExpirada) return;
        if (ordenes != null) aplicarOrdenes(ordenes, res);

        List<RegistroEntity> registros = pedir(api.registros(auth, db.registroDao().maxUpdatedAt()), res);
        if (res.sesionExpirada) return;
        if (registros != null) aplicarRegistros(registros, res);
    }

    // En cada bloque: nunca se pisa una fila con cambios locales sin enviar
    private void aplicarClientes(List<ClienteEntity> lista, SyncResultado res) {
        db.runInTransaction(() -> {
            Set<String> pend = new HashSet<>(db.clienteDao().idsPendientes());
            List<ClienteEntity> aplicar = new ArrayList<>();
            for (ClienteEntity c : lista) {
                if (c.id == null || pend.contains(c.id)) continue;
                c.updatedAt = FechaUtil.normalizar(c.updatedAt);
                c.deletedAt = FechaUtil.normalizar(c.deletedAt);
                c.isSynced = true;
                c.localTs = 0;
                aplicar.add(c);
            }
            db.clienteDao().upsertAll(aplicar);
            res.recibidos += aplicar.size();
        });
    }

    private void aplicarCatalogo(List<CatalogoEntity> lista, SyncResultado res) {
        db.runInTransaction(() -> {
            Set<String> pend = new HashSet<>(db.catalogoDao().idsPendientes());
            List<CatalogoEntity> aplicar = new ArrayList<>();
            for (CatalogoEntity c : lista) {
                if (c.id == null || pend.contains(c.id)) continue;
                c.updatedAt = FechaUtil.normalizar(c.updatedAt);
                c.deletedAt = FechaUtil.normalizar(c.deletedAt);
                c.isSynced = true;
                c.localTs = 0;
                aplicar.add(c);
            }
            db.catalogoDao().upsertAll(aplicar);
            res.recibidos += aplicar.size();
        });
    }

    private void aplicarOrdenes(List<OrdenEntity> lista, SyncResultado res) {
        db.runInTransaction(() -> {
            Set<String> pend = new HashSet<>(db.ordenDao().idsPendientes());
            List<OrdenEntity> aplicar = new ArrayList<>();
            for (OrdenEntity o : lista) {
                if (o.id == null || pend.contains(o.id)) continue;
                o.updatedAt = FechaUtil.normalizar(o.updatedAt);
                o.deletedAt = FechaUtil.normalizar(o.deletedAt);
                o.isSynced = true;
                o.localTs = 0;
                aplicar.add(o);
            }
            db.ordenDao().upsertAll(aplicar);
            res.recibidos += aplicar.size();
        });
    }

    private void aplicarRegistros(List<RegistroEntity> lista, SyncResultado res) {
        db.runInTransaction(() -> {
            Set<String> pend = new HashSet<>(db.registroDao().idsPendientes());
            List<RegistroEntity> aplicar = new ArrayList<>();
            for (RegistroEntity g : lista) {
                if (g.id == null || pend.contains(g.id)) continue;
                g.updatedAt = FechaUtil.normalizar(g.updatedAt);
                g.deletedAt = FechaUtil.normalizar(g.deletedAt);
                g.isSynced = true;
                g.localTs = 0;
                aplicar.add(g);
            }
            db.registroDao().upsertAll(aplicar);
            res.recibidos += aplicar.size();
        });
    }
}