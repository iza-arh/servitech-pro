package com.iza.servitech_pro.ui.taller;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.sync.SyncEstado;
import com.iza.servitech_pro.data.sync.SyncManager;

public class TallerActivity extends AppCompatActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taller);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvTallerNombre)).setText(TallerInfo.NOMBRE);
        ((TextView) findViewById(R.id.tvTallerDireccion)).setText(TallerInfo.DIRECCION);
        findViewById(R.id.btnComoLlegar).setOnClickListener(v -> abrirRuta());

        // Sin conexión no hay mapa: se muestra un aviso y los datos quedan en la tarjeta
        View aviso = findViewById(R.id.tvMapaOffline);
        SyncManager.getInstance(this).estado().observe(this,
                e -> aviso.setVisibility(e == SyncEstado.SIN_CONEXION ? View.VISIBLE : View.GONE));

        SupportMapFragment mapa = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        if (mapa != null) {
            mapa.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng taller = new LatLng(TallerInfo.LAT, TallerInfo.LNG);
        googleMap.addMarker(new MarkerOptions()
                .position(taller)
                .title(TallerInfo.NOMBRE)
                .snippet(TallerInfo.DIRECCION));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taller, 16f));
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    // Abre Google Maps con la ruta; si no está instalado, cualquier app de mapas
    private void abrirRuta() {
        Intent nav = new Intent(Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=" + TallerInfo.LAT + "," + TallerInfo.LNG));
        nav.setPackage("com.google.android.apps.maps");
        try {
            startActivity(nav);
        } catch (ActivityNotFoundException e) {
            Intent geo = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + TallerInfo.LAT + ","
                    + TallerInfo.LNG + "?q=" + TallerInfo.LAT + "," + TallerInfo.LNG
                    + "(" + Uri.encode(TallerInfo.NOMBRE) + ")"));
            try {
                startActivity(geo);
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, R.string.taller_sin_app, Toast.LENGTH_SHORT).show();
            }
        }
    }
}