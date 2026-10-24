package com.iza.servitech_pro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios_local")
public class UsuarioLocal {

    @PrimaryKey
    @NonNull
    public String uid;

    @NonNull
    public String email;

    @NonNull
    public String nombre;

    public String passwordHash;

    public boolean pendienteSync;

    //Constructor.
    public UsuarioLocal(@NonNull String uid, @NonNull String email, @NonNull String nombre, String passwordHash, boolean pendienteSync) {
        this.uid = uid;
        this.email = email;
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.pendienteSync = pendienteSync;
    }
}