package com.iza.servitech_pro.ui.common;

import android.content.Context;

import com.iza.servitech_pro.data.repository.SecurePrefs;

public class Sesion {

    public static boolean esCliente(Context c) {
        return "CLIENTE".equals(SecurePrefs.leerRol(c));
    }

    public static boolean esAdmin(Context c) {
        return "ADMIN".equals(SecurePrefs.leerRol(c));
    }
}