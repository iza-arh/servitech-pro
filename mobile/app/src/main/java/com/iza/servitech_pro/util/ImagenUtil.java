package com.iza.servitech_pro.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImagenUtil {

    private static final int MAX_LADO = 1024;
    private static final int MAX_BYTES = 450_000;

    // Reduce, corrige la rotación y comprime a JPEG. Devuelve el resultado en base64.
    // Llamar fuera del hilo principal.
    public static String comprimirABase64(Context ctx, Uri uri) throws IOException {
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, limites);
        }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) {
            throw new IOException("Imagen inválida");
        }

        int muestra = 1;
        while (Math.max(limites.outWidth, limites.outHeight) / (muestra * 2) >= MAX_LADO) {
            muestra *= 2;
        }
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = muestra;
        Bitmap original;
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            original = BitmapFactory.decodeStream(in, null, opciones);
        }
        if (original == null) {
            throw new IOException("No se pudo leer la imagen");
        }

        int giro = 0;
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            if (in != null) {
                int orientacion = new ExifInterface(in).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (orientacion == ExifInterface.ORIENTATION_ROTATE_90) giro = 90;
                else if (orientacion == ExifInterface.ORIENTATION_ROTATE_180) giro = 180;
                else if (orientacion == ExifInterface.ORIENTATION_ROTATE_270) giro = 270;
            }
        } catch (IOException ignorada) {
            // sin datos EXIF: se deja tal cual
        }

        float escala = Math.min(1f, (float) MAX_LADO / Math.max(original.getWidth(), original.getHeight()));
        Matrix m = new Matrix();
        m.postScale(escala, escala);
        m.postRotate(giro);
        Bitmap finalBmp = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), m, true);

        int calidad = 75;
        byte[] datos;
        do {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            finalBmp.compress(Bitmap.CompressFormat.JPEG, calidad, bos);
            datos = bos.toByteArray();
            calidad -= 10;
        } while (datos.length > MAX_BYTES && calidad >= 35);

        Log.i("SERVITECH_FOTO", "Foto comprimida: " + (datos.length / 1024) + " KB, "
                + finalBmp.getWidth() + "x" + finalBmp.getHeight());
        return Base64.encodeToString(datos, Base64.NO_WRAP);
    }

    // Convierte el base64 en una imagen del tamaño pedido (para miniaturas o vista previa)
    public static Bitmap decodificar(String base64, int maxLado) {
        if (base64 == null) {
            return null;
        }
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, limites);
        int muestra = 1;
        while (Math.max(limites.outWidth, limites.outHeight) / (muestra * 2) >= maxLado) {
            muestra *= 2;
        }
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = muestra;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opciones);
    }
}