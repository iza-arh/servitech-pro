package com.iza.servitech_pro.data.remote.api;

import com.iza.servitech_pro.data.local.entity.CatalogoEntity;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.data.local.entity.OrdenEntity;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.remote.dto.SyncDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SyncApiService {
    @POST("api/sync")
    Call<SyncDto.Response> sync(@Header("Authorization") String auth, @Body SyncDto.Request body);

    // "since" nulo = solo los activos; con fecha = todo lo cambiado después (incluye borrados)
    @GET("api/clientes")
    Call<List<ClienteEntity>> clientes(@Header("Authorization") String auth, @Query("since") String since);

    @GET("api/catalogo")
    Call<List<CatalogoEntity>> catalogo(@Header("Authorization") String auth, @Query("since") String since);

    @GET("api/ordenes")
    Call<List<OrdenEntity>> ordenes(@Header("Authorization") String auth, @Query("since") String since);

    @GET("api/registros")
    Call<List<RegistroEntity>> registros(@Header("Authorization") String auth, @Query("since") String since);
}