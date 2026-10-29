package com.iza.servitech_pro.data.remote.api;

import com.iza.servitech_pro.data.remote.dto.OrdenDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
public interface OrdenApiService {
    @GET("api/ordenes")
    Call<List<OrdenDto>> listar();
}
