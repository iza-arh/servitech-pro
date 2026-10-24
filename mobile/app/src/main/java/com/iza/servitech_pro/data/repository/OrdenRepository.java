package com.iza.servitech_pro.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.iza.servitech_pro.data.local.dao.OrdenTrabajoDao;
import com.iza.servitech_pro.data.local.entity.OrdenTrabajo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrdenRepository {

    private static final String TAG = "OrdenRepository";

    public interface SyncCallback {
        void onFinished(boolean correcto, int pendientesRestantes);
    }

    private final OrdenTrabajoDao ordenDao;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public OrdenRepository(OrdenTrabajoDao ordenDao) {
        this.ordenDao = ordenDao;
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Mantiene compatibilidad con el formulario existente.
    public void sincronizarPendientes() {
        sincronizarPendientes(null);
    }

    public void sincronizarPendientes(SyncCallback callback) {
        executor.execute(() -> {
            boolean correcto = true;

            try {
                // 1. Enviar primero las órdenes locales pendientes.
                List<OrdenTrabajo> pendientes =
                        ordenDao.obtenerPendientes();

                for (OrdenTrabajo orden : pendientes) {
                    try {
                        var documento = firestore.collection("ordenes")
                                .document(orden.id);

                        if (orden.eliminado) {
                            Tasks.await(documento.delete());
                            ordenDao.eliminarFisicamente(orden.id);
                        } else {
                            Map<String, Object> datos = new HashMap<>();
                            datos.put("id", orden.id);
                            datos.put("placa", orden.placa);
                            datos.put("marca", orden.marca);
                            datos.put("modelo", orden.modelo);
                            datos.put("descripcion", orden.descripcion);
                            datos.put("estado", orden.estado);
                            datos.put("uidCreador", orden.uidCreador);

                            Tasks.await(documento.set(datos));

                            orden.pendienteSync = false;
                            ordenDao.actualizar(orden);
                        }

                    } catch (Exception e) {
                        correcto = false;
                        Log.e(TAG,
                                "Error al enviar orden " + orden.id, e);
                    }
                }

                // 2. Descargar las órdenes de Firebase.
                List<DocumentSnapshot> documentos = Tasks.await(
                        firestore.collection("ordenes").get()
                ).getDocuments();

                for (DocumentSnapshot doc : documentos) {
                    try {
                        String id = doc.getId();
                        OrdenTrabajo local = ordenDao.obtenerPorId(id);

                        // No sobrescribir cambios locales pendientes.
                        if (local != null && local.pendienteSync) {
                            continue;
                        }

                        String placa = doc.getString("placa");
                        String marca = doc.getString("marca");
                        String modelo = doc.getString("modelo");
                        String descripcion = doc.getString("descripcion");
                        String estado = doc.getString("estado");
                        String uid = doc.getString("uidCreador");

                        if (placa == null || marca == null
                                || modelo == null || descripcion == null
                                || estado == null || uid == null) {
                            Log.w(TAG,
                                    "Documento incompleto omitido: " + id);
                            correcto = false;
                            continue;
                        }

                        OrdenTrabajo remota = new OrdenTrabajo(
                                id,
                                placa,
                                marca,
                                modelo,
                                descripcion,
                                estado,
                                uid,
                                false,
                                false
                        );

                        ordenDao.insertar(remota);

                    } catch (Exception e) {
                        correcto = false;
                        Log.e(TAG,
                                "Error al descargar orden " + doc.getId(), e);
                    }
                }

            } catch (Exception e) {
                correcto = false;
                Log.e(TAG, "Error general de sincronización", e);
            }

            int restantes = -1;

            try {
                restantes = ordenDao.obtenerPendientes().size();
            } catch (Exception e) {
                correcto = false;
                Log.e(TAG, "No se pudo contar pendientes", e);
            }

            final boolean resultado = correcto;
            final int cantidad = restantes;

            if (callback != null) {
                mainHandler.post(() ->
                        callback.onFinished(resultado, cantidad));
            }
        });
    }
}

