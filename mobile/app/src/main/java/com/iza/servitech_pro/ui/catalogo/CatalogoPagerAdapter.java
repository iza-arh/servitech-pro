package com.iza.servitech_pro.ui.catalogo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.iza.servitech_pro.ui.catalogo.clientes.ClientesFragment;

public class CatalogoPagerAdapter extends FragmentStateAdapter {

    // Mismo orden que las pestañas. null = pestaña de clientes
    public static final String[] TIPOS = {
            null,
            "TIPO_SERVICIO",
            "CATEGORIA_EQUIPO",
            "REPUESTO"
    };

    public CatalogoPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (TIPOS[position] == null) return new ClientesFragment();
        return CatalogoItemsFragment.newInstance(TIPOS[position]);
    }

    @Override
    public int getItemCount() {
        return TIPOS.length;
    }
}
