package com.servitech.authservice.dto;

public class SyncResult {
    public String id;
    public String entidad;
    public String operacion;
    public boolean ok;
    public int status; // 2xx ok, 4xx rechazo definitivo, 5xx reintentar
    public String mensaje; // motivo si fue rechazado
}