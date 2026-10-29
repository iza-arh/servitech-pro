package com.iza.servitech_pro.data.remote.api;

import com.iza.servitech_pro.data.remote.dto.ClienteDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ClienteApiService {
    @GET("api/clientes")
    Call<List<ClienteDto>> listar();
}