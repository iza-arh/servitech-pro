
package com.iza.servitech_pro.ui.ordenes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.entity.OrdenTrabajo;

import java.util.ArrayList;
import java.util.List;

public class OrdenTrabajoAdapter
        extends RecyclerView.Adapter<OrdenTrabajoAdapter.OrdenViewHolder> {

    public interface OnOrdenClickListener {
        void onOrdenClick(OrdenTrabajo orden);
    }

    private final List<OrdenTrabajo> ordenes = new ArrayList<>();
    private final OnOrdenClickListener listener;

    public OrdenTrabajoAdapter(OnOrdenClickListener listener) {
        this.listener = listener;
    }

    public void setOrdenes(List<OrdenTrabajo> nuevasOrdenes) {
        ordenes.clear();
        ordenes.addAll(nuevasOrdenes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrdenViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_orden_trabajo, parent, false);

        return new OrdenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull OrdenViewHolder holder, int position) {

        OrdenTrabajo orden = ordenes.get(position);

        holder.tvVehiculo.setText(
                orden.marca + " " + orden.modelo + " - " + orden.placa
        );
        holder.tvDescripcion.setText(orden.descripcion);
        holder.tvEstado.setText(orden.estado);

        holder.itemView.setOnClickListener(
                v -> listener.onOrdenClick(orden)
        );
    }

    @Override
    public int getItemCount() {
        return ordenes.size();
    }

    static class OrdenViewHolder extends RecyclerView.ViewHolder {

        TextView tvVehiculo;
        TextView tvDescripcion;
        TextView tvEstado;

        OrdenViewHolder(@NonNull View itemView) {
            super(itemView);

            tvVehiculo = itemView.findViewById(R.id.tvVehiculo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvEstado = itemView.findViewById(R.id.tvEstado);
        }
    }
}
