package com.iza.servitech_pro.util;

import android.util.Patterns;

import java.util.regex.Pattern;

public class Validador {

    // Mínimo 8 caracteres, solo letras y números, con al menos una letra y un número
    private static final Pattern PASSWORD =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9]{8,}$");

    public static boolean esCorreoValido(String correo) {
        return correo != null && Patterns.EMAIL_ADDRESS.matcher(correo).matches();
    }

    public static boolean esPasswordValida(String password) {
        return password != null && PASSWORD.matcher(password).matches();
    }

    // Nombre completo: al menos 7 caracteres (sin contar espacios de los extremos)
    public static boolean esNombreValido(String nombre) {
        return nombre != null && nombre.trim().length() >= 7;
    }
}