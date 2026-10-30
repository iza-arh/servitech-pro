package com.iza.servitech_pro.data.sync;

import android.content.Context;
import android.util.Log;

import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.data.local.entity.OrdenEntity;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.local.entity.SyncQueueItem;
import com.iza.servitech_pro.data.repository.LocalStore;

public class SyncSmokeTest {

    private static final String TAG = "SERVITECH_SYNC";
    private static int fallos;

    public static void ejecutar(Context ctx) {
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                correr(app);
            } catch (Exception e) {
                Log.e(TAG, "EXCEPCION", e);
            }
        }).start();
    }

    private static void check(String nombre, boolean ok) {
        Log.i(TAG, (ok ? "OK    " : "FALLO ") + nombre);
        if (!ok) {
            fallos++;
        }
    }

    private static SyncQueueItem enCola(AppDatabase db, String entidad, String id) {
        for (SyncQueueItem q : db.syncQueueDao().getAllItems()) {
            if (q.entidad.equals(entidad) && q.entidadId.equals(id)) {
                return q;
            }
        }
        return null;
    }

    private static OrdenEntity nueva(String id, String clienteId, String placa) {
        OrdenEntity o = new OrdenEntity();
        o.id = id;
        o.clienteId = clienteId;
        o.placa = placa;
        o.descripcion = "Prueba desde la app";
        o.estado = "PENDIENTE";
        return o;
    }

    private static void correr(Context ctx) throws Exception {
        fallos = 0;
        AppDatabase db = AppDatabase.getInstance(ctx);
        LocalStore store = new LocalStore(ctx);
        SyncManager sm = SyncManager.getInstance(ctx);

        final String O1 = "e0000000-0000-4000-8000-0000000000a1";
        final String O2 = "e0000000-0000-4000-8000-0000000000a2";
        final String O3 = "e0000000-0000-4000-8000-0000000000a3";
        final String B1 = "e0000000-0000-4000-8000-0000000000b1";

        // 1) Descarga inicial
        SyncResultado r = sm.sincronizarBloqueante();
        check("1 sync inicial sin error", r != null && r.error == null);
        if (r == null || r.error != null) {
            Log.e(TAG, "ABORTADO. Motivo: " + (r == null ? "sync ocupada" : r.error));
            return;
        }
        check("2 clientes descargados", db.clienteDao().maxUpdatedAt() != null);
        check("3 catalogo descargado", db.catalogoDao().maxUpdatedAt() != null);
        ClienteEntity cli = db.clienteDao().primero();
        if (cli == null) {
            check("hay un cliente local para la prueba", false);
            return;
        }

        // 4-6) Envío de una orden y una bitácora
        store.guardarOrden(nueva(O1, cli.id, "SYNC-APP-1"));
        RegistroEntity b = new RegistroEntity();
        b.id = B1;
        b.ordenId = O1;
        b.tipo = "BITACORA";
        b.texto = "Registro desde la app";
        store.guardarRegistro(b);

        r = sm.sincronizarBloqueante();
        check("4 enviados=2 rechazados=0", r != null && r.error == null && r.enviados == 2 && r.rechazados == 0);
        OrdenEntity o = db.ordenDao().getById(O1);
        check("5 orden sincronizada y con fecha del servidor", o.isSynced && o.updatedAt != null);
        check("6 cola sin pendientes de esa orden",
                enCola(db, "ORDEN", O1) == null);

        // 7-8) Rechazo del servidor
        store.guardarOrden(nueva(O3, "d0000000-0000-4000-8000-0000000000e1", "SYNC-MALA"));
        r = sm.sincronizarBloqueante();
        check("7 orden con cliente falso rechazada", r != null && r.rechazados == 1);
        SyncQueueItem rech = enCola(db, "ORDEN", O3);
        check("8 motivo guardado en la cola", rech != null && "RECHAZADO".equals(rech.estado) && rech.ultimoError != null);
        if (rech != null) {
            Log.i(TAG, "Motivo del rechazo: " + rech.ultimoError);
        }

        // 9-12) Sin red (simulada) y recuperación
        sm.simularSinRed = true;
        store.guardarOrden(nueva(O2, cli.id, "SYNC-APP-2"));
        r = sm.sincronizarBloqueante();
        check("9 sin red: la sync falla", r != null && r.error != null);
        SyncQueueItem q2 = enCola(db, "ORDEN", O2);
        check("10 la orden sigue pendiente (no se perdio)",
                !db.ordenDao().getById(O2).isSynced && q2 != null && "PENDIENTE".equals(q2.estado));

        sm.simularSinRed = false; // vuelve la "red"
        r = sm.sincronizarBloqueante();
        check("11 al volver la red se envia lo pendiente", r != null && r.error == null && r.enviados >= 1);
        check("12 la orden quedo sincronizada", db.ordenDao().getById(O2).isSynced);

        Log.i(TAG, fallos == 0 ? "RESULTADO: TODOS OK" : "RESULTADO: " + fallos + " FALLOS");
    }
}