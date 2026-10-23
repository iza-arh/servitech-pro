package com.iza.servitech_pro.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecurePrefs {

    private static final String KEY_ALIAS = "servitech_pro_key";
    private static final String PREFS_NAME = "secure_session";

    private static SecretKey obtenerOCrearClave() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator generador = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generador.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            generador.generateKey();
        }
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    public static void guardarSesion(Context context, String token, String rol) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, obtenerOCrearClave());
            byte[] iv = cipher.getIV();
            byte[] tokenCifrado = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putString("token_cifrado", Base64.encodeToString(tokenCifrado, Base64.NO_WRAP))
                    .putString("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                    .putString("rol", rol)
                    .apply();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la sesión", e);
        }
    }

    public static String leerToken(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String tokenB64 = prefs.getString("token_cifrado", null);
            String ivB64 = prefs.getString("iv", null);
            if (tokenB64 == null || ivB64 == null) return null;

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, obtenerOCrearClave(),
                    new GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(tokenB64, Base64.NO_WRAP)),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null; // sesión ilegible: se trata como no autenticado
        }
    }

    public static String leerRol(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("rol", null);
    }

    public static void cerrarSesion(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static final String PREFS_RECUERDO = "recordarme";

    public static void guardarCredenciales(Context context, String email, String password) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, obtenerOCrearClave());
            byte[] iv = cipher.getIV();
            byte[] cifrado = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

            context.getSharedPreferences(PREFS_RECUERDO, Context.MODE_PRIVATE).edit()
                    .putBoolean("activo", true)
                    .putString("email", email)
                    .putString("password_cifrada", Base64.encodeToString(cifrado, Base64.NO_WRAP))
                    .putString("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                    .apply();
        } catch (Exception e) {
            borrarCredenciales(context); // si falla el cifrado, no se guarda nada
        }
    }

    public static boolean recordarActivo(Context context) {
        return context.getSharedPreferences(PREFS_RECUERDO, Context.MODE_PRIVATE)
                .getBoolean("activo", false);
    }

    public static String leerEmailRecordado(Context context) {
        return context.getSharedPreferences(PREFS_RECUERDO, Context.MODE_PRIVATE)
                .getString("email", "");
    }

    public static String leerPasswordRecordada(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_RECUERDO, Context.MODE_PRIVATE);
            String dataB64 = prefs.getString("password_cifrada", null);
            String ivB64 = prefs.getString("iv", null);
            if (dataB64 == null || ivB64 == null) return "";

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, obtenerOCrearClave(),
                    new GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(dataB64, Base64.NO_WRAP)),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static void borrarCredenciales(Context context) {
        context.getSharedPreferences(PREFS_RECUERDO, Context.MODE_PRIVATE).edit().clear().apply();
    }
}