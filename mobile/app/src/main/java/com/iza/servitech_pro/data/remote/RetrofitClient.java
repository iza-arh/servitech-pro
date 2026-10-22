package com.iza.servitech_pro.data.remote;

import com.iza.servitech_pro.data.remote.api.AuthApiService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
//
public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.1.94:8080/";
    private static Retrofit retrofit;

    public static AuthApiService getAuthApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(AuthApiService.class);
    }
}
