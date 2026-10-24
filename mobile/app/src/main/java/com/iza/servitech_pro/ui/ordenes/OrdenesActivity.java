
package com.iza.servitech_pro.ui.ordenes;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.iza.servitech_pro.R;

public class OrdenesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ordenes);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.contenedorOrdenes,
                            new ListadoOrdenesFragment()
                    )
                    .commit();
        }
    }

    public void mostrarFormulario(String ordenId) {
        FormularioOrdenFragment fragment =
                FormularioOrdenFragment.newInstance(ordenId);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedorOrdenes, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void mostrarDetalle(String ordenId) {
        DetalleOrdenFragment fragment =
                DetalleOrdenFragment.newInstance(ordenId);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedorOrdenes, fragment)
                .addToBackStack(null)
                .commit();
    }
}

