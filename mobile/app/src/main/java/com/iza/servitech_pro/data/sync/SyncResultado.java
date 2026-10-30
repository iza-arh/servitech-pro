package com.iza.servitech_pro.data.sync;

public class SyncResultado {
    public int enviados;      // aceptados por el servidor
    public int rechazados;    // rechazados (quedan en la cola con el motivo)
    public int recibidos;     // filas descargadas
    public String error;      // null = todo bien
    public boolean sesionExpirada;
    public long fecha = System.currentTimeMillis();

    public boolean ok() {
        return error == null && !sesionExpirada;
    }
}