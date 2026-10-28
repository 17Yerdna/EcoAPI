package pe.upeu.ecorecicla.dominio

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

@Entity @Table(name="punto_acopio")
open class PuntoAcopio(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) open var id: Long = 0,
    open var razonSocial: String = "",
    open var direccion: String = "",
    open var zona: String = "",
    open var materialHabitual: String = ""
)
@Entity @Table(name="ruta")
open class Ruta(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) open var id: Long = 0,
    open var fecha: LocalDate = LocalDate.now(),
    open var cerrada: Boolean = false
)
@Entity @Table(name="ruta_punto")
open class RutaPunto(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) open var id: Long = 0,
    open var rutaId: Long = 0,
    open var puntoId: Long = 0,
    open var secuencia: Int = 0,
    open var motivoOmision: String? = null
)
@Entity @Table(name="pesaje")
open class Pesaje(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) open var id: Long = 0,
    open var rutaId: Long = 0,
    open var puntoId: Long = 0,
    open var fechaHora: Instant = Instant.now(),
    open var material: String = "",
    open var pesoBruto: Double = 0.0,
    open var tara: Double = 0.0,
    open var pesoNeto: Double = 0.0,
    open var operarioId: Long = 0,
    open var observacion: String = "",
    open var anulado: Boolean = false,
    open var motivoAnulacion: String? = null,
    open var anuladoPor: Long? = null,
    open var anuladoEn: Instant? = null,
    @Column(unique=true) open var claveCliente: String? = null,
    open var humedadEstimada: Double = 0.0
)
