package com.iza.servitech_pro.data.remote;

import android.content.Context;

import com.iza.servitech_pro.data.remote.api.AuthApiService;
import com.iza.servitech_pro.data.remote.api.ClienteApiService;
import com.iza.servitech_pro.data.remote.api.OrdenApiService;
import com.iza.servitech_pro.data.remote.api.SyncApiService;
import com.iza.servitech_pro.data.repository.SecurePrefs;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
//
public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.1.94:8080/";
    private static Retrofit retrofit;
    private static Retrofit retrofitConToken;
    private static Retrofit retrofitSync;

    public static AuthApiService getAuthApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(AuthApiService.class);
    }

    private static Retrofit conToken(Context context) {
        final Context app = context.getApplicationContext();
        if (retrofitConToken == null) {
            OkHttpClient http = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        // Agrega el token guardado a cada petición
                        String token = SecurePrefs.leerToken(app);
                        Request original = chain.request();
                        if (token == null) {
                            return chain.proceed(original);
                        }
                        return chain.proceed(original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build());
                    })
                    .build();
            retrofitConToken = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // la misma constante que ya usas
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitConToken;
    }

    public static ClienteApiService getClienteApi(Context context) {
        return conToken(context).create(ClienteApiService.class);
    }

    public static OrdenApiService getOrdenApi(Context context) {
        return conToken(context).create(OrdenApiService.class);
    }

    // Sincronización: timeouts largos porque los lotes pueden llevar fotos
    // (el token se manda en el parámetro Authorization de cada método)
    public static synchronized SyncApiService getSyncApi() {
        if (retrofitSync == null) {
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();
            retrofitSync = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitSync.create(SyncApiService.class);
    }
}
