package pe.upeu.ecorecicla.servicio

import java.time.LocalDate

class ErrorNegocio(val status: Int, override val message: String, val campos: Map<String,String> = emptyMap()) : RuntimeException(message)

object ReglasPesaje {
    fun neto(bruto: Double, tara: Double): Double {
        if (!bruto.isFinite() || !tara.isFinite() || tara < 0 || bruto <= tara)
            throw ErrorNegocio(400, "El peso neto debe ser mayor que cero", mapOf("pesoBruto" to "Debe ser mayor que la tara", "tara" to "Debe ser no negativa y menor que el bruto"))
        return bruto - tara
    }
    fun rutaAbierta(cerrada: Boolean) {
        if (cerrada) throw ErrorNegocio(409,"La ruta ya está cerrada")
    }
    fun puedeCorregir(usuarioId: Long, operarioId: Long, fecha: LocalDate, hoy: LocalDate, global: Boolean) {
        if (!global && (usuarioId != operarioId || fecha != hoy))
            throw ErrorNegocio(403,"Solo puede corregir pesajes propios del mismo día")
    }
    fun puedeCerrar(pendientes: Int) {
        if (pendientes > 0) throw ErrorNegocio(409,"Faltan $pendientes puntos sin pesaje ni motivo de omisión")
    }
}
