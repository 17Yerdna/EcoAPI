package pe.upeu.ecorecicla.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import pe.upeu.ecorecicla.servicio.EcoService
import pe.upeu.ecorecicla.web.dto.*
import java.time.LocalDate

@RestController @RequestMapping("/api/v1")
class EcoController(private val service: EcoService) {
    @PostMapping("/auth/login") fun login(@Valid @RequestBody c: CredencialesRequest) = service.login(c)
    @GetMapping("/usuarios/me") fun perfil() = service.principal()
    @GetMapping("/rutas/dia") fun ruta(@RequestParam(required=false) fecha: LocalDate?) = service.rutaDia(fecha)
    @GetMapping("/puntos") fun puntos(@RequestParam(required=false) zona: String?,@RequestParam(defaultValue="0") pagina: Int,@RequestParam(defaultValue="20") tamanio: Int) = service.puntos(zona,pagina,tamanio)
    @PostMapping("/puntos") @ResponseStatus(HttpStatus.CREATED) fun punto(@Valid @RequestBody c: CrearPuntoRequest) = service.crearPunto(c)
    @PostMapping("/rutas/{rutaId}/puntos/{puntoId}") @ResponseStatus(HttpStatus.CREATED) fun asignar(@PathVariable rutaId: Long,@PathVariable puntoId: Long) = service.asignar(rutaId,puntoId)
    @GetMapping("/puntos/{id}/pesajes") fun historial(@PathVariable id: Long,@RequestParam(defaultValue="0") pagina: Int,@RequestParam(defaultValue="20") tamanio: Int) = service.historial(id,pagina,tamanio)
    @PostMapping("/pesajes") @ResponseStatus(HttpStatus.CREATED) fun registrar(@Valid @RequestBody c: PesajeRequest) = service.registrar(c)
    @PutMapping("/pesajes/{id}") fun corregir(@PathVariable id: Long,@Valid @RequestBody c: PesajeRequest) = service.corregir(id,c)
    @DeleteMapping("/pesajes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) fun anular(@PathVariable id: Long,@RequestParam motivo: String) = service.anular(id,motivo)
    @PutMapping("/rutas/{rutaId}/puntos/{puntoId}/omision") fun omitir(@PathVariable rutaId: Long,@PathVariable puntoId: Long,@Valid @RequestBody c: MotivoRequest) = service.omitir(rutaId,puntoId,c.motivo)
    @PostMapping("/rutas/{id}/cierre") fun cerrar(@PathVariable id: Long) = service.cerrar(id)
}
