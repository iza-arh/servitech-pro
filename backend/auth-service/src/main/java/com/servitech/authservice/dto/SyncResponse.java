package com.servitech.authservice.dto;

import java.util.List;

public class SyncResponse {
    public int aceptados;
    public int rechazados;
    public List<SyncResult> resultados;
}