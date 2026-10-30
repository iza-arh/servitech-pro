package com.iza.servitech_pro.data.remote.dto;

import java.util.List;

// Formas JSON del endpoint /api/sync (igual que el servidor)
public class SyncDto {

    public static class Request {
        public List<Item> items;
    }

    public static class Item {
        public String entidad;    // CLIENTE, CATALOGO, ORDEN, REGISTRO
        public String operacion;  // UPSERT, DELETE
        public String id;
        public ClienteReq cliente;
        public CatalogoReq catalogo;
        public OrdenReq orden;
        public RegistroReq registro;
    }

    public static class ClienteReq {
        public String nombre, telefono, correo, direccion;
    }

    public static class CatalogoReq {
        public String tipo, nombre, descripcion, codigo;
        public Double precio;
    }

    public static class OrdenReq {
        public String clienteId, tipoServicioId, categoriaEquipoId;
        public String placa, marca, modelo, descripcion, estado;
        public Long tecnicoId;
        public Integer anio;
    }

    public static class RegistroReq {
        public String ordenId, tipo, texto, catalogoItemId, datos;
        public Integer cantidad;
    }

    public static class Result {
        public String id, entidad, operacion, mensaje;
        public boolean ok;
        public int status;
    }

    public static class Response {
        public int aceptados, rechazados;
        public List<Result> resultados;
    }
}
