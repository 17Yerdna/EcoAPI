package pe.upeu.ecorecicla.web.dto

import jakarta.validation.constraints.*
import org.springframework.data.domain.Page
import java.time.Instant

data class CredencialesRequest(@field:NotBlank val usuario: String, @field:NotBlank val contrasena: String)
data class PesajeRequest(
    val puntoId: Long? = null,
    @field:NotBlank @field:Size(max=80) val material: String,
    val pesoBruto: Double, val tara: Double,
    @field:Size(max=500) val observacion: String = "",
    @field:Size(max=100) val claveCliente: String? = null
)
data class CrearPuntoRequest(
    @field:NotBlank @field:Size(max=180) val razonSocial: String,
    @field:NotBlank @field:Size(max=250) val direccion: String,
    @field:NotBlank @field:Size(max=80) val zona: String,
    @field:NotBlank @field:Size(max=80) val materialHabitual: String
)
data class MotivoRequest(@field:NotBlank @field:Size(max=500) val motivo: String)
data class ErrorResponse(val timestamp: String = Instant.now().toString(), val status: Int, val error: String, val message: String, val path: String, val validationErrors: Map<String,String> = emptyMap())
data class PaginaResponse<T>(val contenido: List<T>, val pagina: Int, val tamanio: Int, val totalElementos: Long, val totalPaginas: Int, val ultima: Boolean) {
    companion object { fun <T: Any> de(p: Page<T>) = PaginaResponse(p.content,p.number,p.size,p.totalElements,p.totalPages,p.isLast) }
}
