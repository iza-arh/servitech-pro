package com.iza.servitech_pro.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.ui.login.LoginActivity;
import com.iza.servitech_pro.ui.mapa.MapaActivity;
import com.iza.servitech_pro.ui.ordenes.OrdenesActivity;
import com.iza.servitech_pro.ui.perfil.PerfilActivity;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        setSupportActionBar(findViewById(R.id.toolbar));

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        TextView tvBienvenida = findViewById(R.id.tvBienvenida);
        tvBienvenida.setText("Bienvenido, " + (usuario != null ? usuario.getEmail() : ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_ordenes) {
            startActivity(new Intent(this, OrdenesActivity.class));
            return true;
        } else if (id == R.id.action_mapa) {
            startActivity(new Intent(this, MapaActivity.class));
            return true;
        } else if (id == R.id.action_perfil) {
            startActivity(new Intent(this, PerfilActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}