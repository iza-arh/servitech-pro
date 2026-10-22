package com.iza.servitech_pro.data.remote.api;

import com.iza.servitech_pro.data.remote.dto.LoginRequest;
import com.iza.servitech_pro.data.remote.dto.LoginResponse;
import com.iza.servitech_pro.data.remote.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<Void> register(@Body RegisterRequest request);
}
