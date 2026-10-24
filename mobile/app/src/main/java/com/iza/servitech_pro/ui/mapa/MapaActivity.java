package com.iza.servitech_pro.ui.mapa;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.iza.servitech_pro.R;

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    // TODO: cambiar por los datos reales del negocio
    private static final String NOMBRE_NEGOCIO = "Servitech Pro - Taller";
    private static final double LATITUD = 13.6929;
    private static final double LONGITUD = -89.2182;
    private static final float ZOOM = 16f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ubicación del taller");
        }

        SupportMapFragment fragmento = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (fragmento != null) {
            fragmento.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap mapa) {
        LatLng punto = new LatLng(LATITUD, LONGITUD);
        mapa.addMarker(new MarkerOptions().position(punto).title(NOMBRE_NEGOCIO));
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(punto, ZOOM));
        mapa.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}