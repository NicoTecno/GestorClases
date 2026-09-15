package com.nico.gestorclases.data.model

enum class NivelEducativo(val displayName: String) {
    PRIMARIA("Primaria"),
    SECUNDARIA("Secundaria"),
    UNIVERSIDAD("Universidad")
}

enum class EstadoClase(val displayName: String) {
    RESERVADA("Reservada"),
    DADA("Dada"),
    CANCELADA_CON_TIEMPO("Cancelada con tiempo"),
    CANCELADA_MISMO_DIA("Cancelada el mismo día")
}

enum class EstadoPago(val displayName: String) {
    PENDIENTE("Pendiente"),
    PAGADA("Pagada")
}
