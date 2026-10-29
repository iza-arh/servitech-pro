package com.servitech.authservice.dto;

public class SyncItem {
    public String entidad; // CLIENTE, CATALOGO, ORDEN, REGISTRO
    public String operacion; // UPSERT, DELETE
    public String id; // UUID del registro

    // Solo se llena el que corresponde a "entidad"
    public ClienteRequest cliente;
    public CatalogoRequest catalogo;
    public OrdenTrabajoRequest orden;
    public RegistroRequest registro;
}