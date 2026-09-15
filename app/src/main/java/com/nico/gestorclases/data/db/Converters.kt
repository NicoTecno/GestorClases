package com.nico.gestorclases.data.db

import androidx.room.TypeConverter
import com.nico.gestorclases.data.model.EstadoClase
import com.nico.gestorclases.data.model.EstadoPago
import com.nico.gestorclases.data.model.NivelEducativo

class Converters {

    // NivelEducativo
    @TypeConverter
    fun fromNivelEducativo(value: NivelEducativo): String = value.name

    @TypeConverter
    fun toNivelEducativo(value: String): NivelEducativo = NivelEducativo.valueOf(value)

    // EstadoClase
    @TypeConverter
    fun fromEstadoClase(value: EstadoClase): String = value.name

    @TypeConverter
    fun toEstadoClase(value: String): EstadoClase = EstadoClase.valueOf(value)

    // EstadoPago
    @TypeConverter
    fun fromEstadoPago(value: EstadoPago): String = value.name

    @TypeConverter
    fun toEstadoPago(value: String): EstadoPago = EstadoPago.valueOf(value)
}
