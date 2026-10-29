package com.iza.servitech_pro.data.local;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FechaUtil {

    // Fecha actual en formato del servidor, con 9 decimales (comparable como texto)
    public static String ahora() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date()) + "000000Z";
    }

    // Rellena la parte decimal a 9 dígitos para que el orden por texto sea correcto
    public static String normalizar(String s) {
        if (s == null) {
            return null;
        }
        int z = s.indexOf('Z');
        if (z < 0) {
            return s;
        }
        int punto = s.indexOf('.');
        if (punto < 0) {
            return s.substring(0, z) + ".000000000Z";
        }
        StringBuilder frac = new StringBuilder(s.substring(punto + 1, z));
        while (frac.length() < 9) {
            frac.append('0');
        }
        if (frac.length() > 9) {
            frac.setLength(9);
        }
        return s.substring(0, punto + 1) + frac + "Z";
    }
}