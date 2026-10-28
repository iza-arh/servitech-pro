package com.servitech.authservice.dto;

public class OrdenTrabajoRequest {

    private String clienteId;
    private Long tecnicoId;
    private String placa;
    private String marca;
    private String modelo;
    private Integer anio;
    private String descripcion;
    private String estado;
    private String tipoServicioId;
    private String categoriaEquipoId;

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(Long tecnicoId) {
        this.tecnicoId = tecnicoId;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipoServicioId() {
        return tipoServicioId;
    }

    public void setTipoServicioId(String tipoServicioId) {
        this.tipoServicioId = tipoServicioId;
    }

    public String getCategoriaEquipoId() {
        return categoriaEquipoId;
    }

    public void setCategoriaEquipoId(String categoriaEquipoId) {
        this.categoriaEquipoId = categoriaEquipoId;
    }
}