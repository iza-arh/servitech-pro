package com.iza.servitech_pro.data.remote.api;

import com.iza.servitech_pro.data.remote.dto.UsuarioAdminDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UsuarioAdminApiService {
    @GET("api/admin/usuarios")
    Call<List<UsuarioAdminDto.Usuario>> listar(@Query("rol") String rol);

    @POST("api/admin/usuarios")
    Call<UsuarioAdminDto.Usuario> crear(@Body UsuarioAdminDto.Crear body);

    @PUT("api/admin/usuarios/{id}/password")
    Call<Void> cambiarPassword(@Path("id") long id, @Body UsuarioAdminDto.Password body);
}