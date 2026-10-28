package pe.upeu.ecorecicla.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pe.upeu.ecorecicla.dominio.*
import pe.upeu.ecorecicla.seguridad.Usuario
import java.time.LocalDate

interface UsuarioRepository : JpaRepository<Usuario,Long> { fun findByUsuario(usuario: String): Usuario? }
interface PuntoRepository : JpaRepository<PuntoAcopio,Long> { fun findByZona(zona: String, pageable: Pageable): Page<PuntoAcopio> }
interface RutaRepository : JpaRepository<Ruta,Long> {
    @Query(value="select r.* from ruta r join ruta_usuario u on u.ruta_id=r.id where u.usuario_id=:usuarioId and r.fecha=:fecha",nativeQuery=true)
    fun delDia(usuarioId: Long, fecha: LocalDate): Ruta?
}
interface RutaPuntoRepository : JpaRepository<RutaPunto,Long> {
    fun findByRutaIdOrderBySecuencia(rutaId: Long): List<RutaPunto>
    fun findByRutaIdAndPuntoId(rutaId: Long, puntoId: Long): RutaPunto?
}
interface PesajeRepository : JpaRepository<Pesaje,Long> {
    fun findByPuntoId(puntoId: Long, pageable: Pageable): Page<Pesaje>
    fun existsByRutaIdAndPuntoIdAndAnuladoFalse(rutaId: Long,puntoId: Long): Boolean
    fun findByClaveCliente(claveCliente: String): Pesaje?
}
