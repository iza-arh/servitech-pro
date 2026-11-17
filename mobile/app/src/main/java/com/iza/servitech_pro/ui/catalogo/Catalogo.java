package com.iza.servitech_pro.ui.catalogo;

import android.os.Bundle;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.sync.SyncManager;
import com.iza.servitech_pro.ui.catalogo.clientes.ClienteFormDialog;
import com.iza.servitech_pro.ui.common.SyncBarView;

public class Catalogo extends AppCompatActivity {

    private final String[] titulos = {"Clientes", "Servicios", "Equipos", "Repuestos"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalogo);

        SyncManager.getInstance(this).sincronizar();
        SyncBarView barra = findViewById(R.id.syncBar);
        barra.enlazar(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        FloatingActionButton fab = findViewById(R.id.fabAgregar);

        // Flecha de regreso
        toolbar.setNavigationOnClickListener(v -> finish());

        viewPager.setAdapter(new CatalogoPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(titulos[position])
        ).attach();

        //Para busqueda en catalogo.
        CatalogoViewModel vm = new ViewModelProvider(this).get(CatalogoViewModel.class);
        TextInputEditText etBuscar = findViewById(R.id.etBuscar);

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                vm.setTextoBusqueda(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                etBuscar.setText("");
                etBuscar.clearFocus();
            }
        });

        fab.setOnClickListener(v -> {
            FragmentManager fm = getSupportFragmentManager();
            String tag = "form_nuevo";
            if (fm.findFragmentByTag(tag) != null) return; // evita doble toque

            String tipo = CatalogoPagerAdapter.TIPOS[viewPager.getCurrentItem()];
            if (tipo == null) {
                ClienteFormDialog.newInstance(null).show(fm, tag);
            } else {
                CatalogoFormDialog.newInstance(tipo, null).show(fm, tag);
            }
        });
    }
}