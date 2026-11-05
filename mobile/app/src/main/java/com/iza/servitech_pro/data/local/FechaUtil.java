package com.iza.servitech_pro.data.local;

import java.text.ParseException;
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

    // Fecha para mostrar en pantalla: dd/MM/yyyy
    public static String mostrar(String iso, long localTs) {
        if (iso != null && iso.length() >= 10) {
            return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
        }
        if (localTs > 0) {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(localTs));
        }
        return "";
    }

    // Momento de un registro en milisegundos (fecha del servidor, o la local si aún no se envió)
    public static long aMillis(String iso, long localTs) {
        if (iso != null && iso.length() >= 19) {
            try {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = f.parse(iso.substring(0, 19));
                if (d != null) {
                    return d.getTime();
                }
            } catch (ParseException ignored) {
            }
        }
        return localTs;
    }

    // "08:30 AM · 02/11/2026" en la hora del teléfono
    public static String horaYFecha(long millis) {
        return new SimpleDateFormat("hh:mm a · dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }
}