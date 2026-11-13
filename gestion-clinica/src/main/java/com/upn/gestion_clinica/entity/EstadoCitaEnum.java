package com.upn.gestion_clinica.entity;

public enum EstadoCitaEnum {
    Confirmada,
    Reprogramada,
    Asistió,
    Cancelada,
    No_Asistió;

    public boolean permiteReprogramar() {
        return this == Confirmada || this == Reprogramada;
    }

    public String valorApi() {
        return name().replace('_', ' ');
    }

    public static EstadoCitaEnum desdeApi(String estado) {
        return valueOf(estado.replace(' ', '_'));
    }
}
