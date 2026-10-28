package pe.upeu.ecorecicla.servicio

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import pe.upeu.ecorecicla.repositorio.*
import pe.upeu.ecorecicla.dominio.*
import pe.upeu.ecorecicla.seguridad.Principal
import pe.upeu.ecorecicla.config.JwtService
import pe.upeu.ecorecicla.web.dto.*
import java.time.*
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType

@Service
@Transactional
class EcoService(private val usuarios: UsuarioRepository,private val puntos: PuntoRepository,private val rutas: RutaRepository,private val asignaciones: RutaPuntoRepository,private val pesajes: PesajeRepository,private val db: JdbcTemplate,private val jwt: JwtService,private val encoder: BCryptPasswordEncoder,private val em: EntityManager) {
    private val zona = ZoneId.of("America/Lima")
    private fun hoy() = LocalDate.now(zona)
    fun principal(): Principal = SecurityContextHolder.getContext().authentication?.principal as? Principal ?: throw ErrorNegocio(401,"Sesión requerida")
    private fun exigir(privilegio: String): Principal = principal().also { if(privilegio !in it.privilegios) throw ErrorNegocio(403,"Privilegio requerido: $privilegio") }
    fun login(c: CredencialesRequest): Map<String,Any> {
        val u=usuarios.findByUsuario(c.usuario)
        if (u==null || !encoder.matches(c.contrasena,u.contrasena)) throw ErrorNegocio(401,"Credenciales incorrectas")
        val privilegios=db.queryForList("select privilegio from rol_privilegio where rol=?",String::class.java,u.rol).filterNotNull().toSet()
        val p=Principal(u.id,u.usuario,u.rol,privilegios)
        return mapOf("token" to jwt.generar(p),"usuario" to p.usuario,"rol" to p.rol,"privilegios" to p.privilegios)
    }
    private fun ruta(id: Long): Ruta = rutas.findById(id).orElseThrow { ErrorNegocio(404,"Ruta no encontrada") }
    private fun propia(r: Ruta) {
        if(db.queryForObject("select count(*) from ruta_usuario where ruta_id=? and usuario_id=?",Int::class.java,r.id,principal().id)==0) throw ErrorNegocio(403,"No pertenece a esta ruta")
    }
    private fun bloquearRuta(id: Long): Ruta {
        val r=ruta(id)
        em.refresh(r,LockModeType.PESSIMISTIC_WRITE)
        return r
    }
    fun rutaDia(fecha: LocalDate?): Map<String,Any> {
        val p=exigir("PUNTO_LEER")
        return rutaDto(rutas.delDia(p.id,fecha ?: hoy()) ?: throw ErrorNegocio(404,"No hay ruta asignada para $fecha"))
    }
    private fun rutaDto(r: Ruta): Map<String,Any> = mapOf("id" to r.id,"fecha" to r.fecha.toString(),"cerrada" to r.cerrada,"puntos" to asignaciones.findByRutaIdOrderBySecuencia(r.id).map { rp ->
        val p=puntos.findById(rp.puntoId).orElseThrow { ErrorNegocio(404,"Punto no encontrado") }
        mapOf("id" to p.id,"razonSocial" to p.razonSocial,"direccion" to p.direccion,"zona" to p.zona,"materialHabitual" to p.materialHabitual,"secuencia" to rp.secuencia,"atendido" to (pesajes.existsByRutaIdAndPuntoIdAndAnuladoFalse(r.id,p.id) || !rp.motivoOmision.isNullOrBlank()),"motivoOmision" to (rp.motivoOmision ?: ""))
    })
    private fun pagina(p: Int,t: Int): PageRequest {
        if(p<0 || t !in 1..100) throw ErrorNegocio(400,"Paginación inválida")
        return PageRequest.of(p,t,Sort.by("id"))
    }
    fun puntos(zona: String?,p: Int,t: Int): PaginaResponse<PuntoAcopio> {
        exigir("PUNTO_LEER")
        return PaginaResponse.de(if(zona.isNullOrBlank()) puntos.findAll(pagina(p,t)) else puntos.findByZona(zona,pagina(p,t)))
    }
    fun crearPunto(c: CrearPuntoRequest): PuntoAcopio {
        exigir("PUNTO_GESTIONAR")
        return puntos.save(PuntoAcopio(razonSocial=c.razonSocial,direccion=c.direccion,zona=c.zona,materialHabitual=c.materialHabitual))
    }
    fun asignar(rutaId: Long,puntoId: Long): Map<String,Any> {
        exigir("PUNTO_GESTIONAR")
        val r=bloquearRuta(rutaId); ReglasPesaje.rutaAbierta(r.cerrada)
        if(!puntos.existsById(puntoId)) throw ErrorNegocio(404,"Punto no encontrado")
        val existentes=asignaciones.findByRutaIdOrderBySecuencia(rutaId)
        if(existentes.any { it.puntoId==puntoId }) throw ErrorNegocio(409,"Punto ya asignado")
        asignaciones.saveAndFlush(RutaPunto(rutaId=rutaId,puntoId=puntoId,secuencia=(existentes.maxOfOrNull { it.secuencia } ?: 0)+1))
        return rutaDto(r)
    }
    private fun pesajeDto(v: Pesaje): Map<String,Any> = mapOf("id" to v.id,"puntoId" to v.puntoId,"fechaHora" to v.fechaHora.atZone(zona).toOffsetDateTime().toString(),"material" to v.material,"pesoBruto" to v.pesoBruto,"tara" to v.tara,"pesoNeto" to v.pesoNeto,"operario" to usuarios.findById(v.operarioId).orElseThrow().usuario,"estado" to if(v.anulado) mapOf("tipo" to "ANULADO","motivo" to (v.motivoAnulacion ?: "")) else mapOf("tipo" to "REGISTRADO"),"observacion" to v.observacion,"pendiente" to false)
    fun historial(puntoId: Long,p: Int,t: Int): PaginaResponse<Map<String,Any>> {
        exigir("PESAJE_LEER")
        if(!puntos.existsById(puntoId)) throw ErrorNegocio(404,"Punto no encontrado")
        return PaginaResponse.de(pesajes.findByPuntoId(puntoId,pagina(p,t)).map(::pesajeDto))
    }
    fun registrar(c: PesajeRequest): Map<String,Any> {
        val u=exigir("PESAJE_CREAR")
        val neto=ReglasPesaje.neto(c.pesoBruto,c.tara)
        c.claveCliente?.let { key -> pesajes.findByClaveCliente(key)?.let { if(it.operarioId!=u.id) throw ErrorNegocio(409,"Clave de operación ya utilizada"); return pesajeDto(it) } }
        val puntoId=c.puntoId ?: throw ErrorNegocio(400,"Falta puntoId",mapOf("puntoId" to "Requerido"))
        val r=rutas.delDia(u.id,hoy()) ?: throw ErrorNegocio(404,"No hay ruta asignada")
        val locked=bloquearRuta(r.id); ReglasPesaje.rutaAbierta(locked.cerrada)
        if(asignaciones.findByRutaIdAndPuntoId(r.id,puntoId)==null) throw ErrorNegocio(404,"Punto fuera de la ruta")
        return pesajeDto(pesajes.saveAndFlush(Pesaje(rutaId=r.id,puntoId=puntoId,material=c.material,pesoBruto=c.pesoBruto,tara=c.tara,pesoNeto=neto,operarioId=u.id,observacion=c.observacion,claveCliente=c.claveCliente)))
    }
    fun corregir(id: Long,c: PesajeRequest): Map<String,Any> {
        val u=exigir("PESAJE_EDITAR")
        val v=pesajes.findById(id).orElseThrow { ErrorNegocio(404,"Pesaje no encontrado") }
        val r=bloquearRuta(v.rutaId); if("PESAJE_ANULAR" !in u.privilegios) propia(r); ReglasPesaje.rutaAbierta(r.cerrada)
        ReglasPesaje.puedeCorregir(u.id,v.operarioId,v.fechaHora.atZone(zona).toLocalDate(),hoy(),"PESAJE_ANULAR" in u.privilegios)
        if(v.anulado) throw ErrorNegocio(409,"El pesaje está anulado")
        v.pesoNeto=ReglasPesaje.neto(c.pesoBruto,c.tara); v.material=c.material; v.pesoBruto=c.pesoBruto; v.tara=c.tara; v.observacion=c.observacion
        return pesajeDto(pesajes.saveAndFlush(v))
    }
    fun anular(id: Long,motivo: String) {
        val u=exigir("PESAJE_ANULAR")
        if(motivo.isBlank() || motivo.length>500) throw ErrorNegocio(400,"Motivo requerido",mapOf("motivo" to "Entre 1 y 500 caracteres"))
        val v=pesajes.findById(id).orElseThrow { ErrorNegocio(404,"Pesaje no encontrado") }
        v.anulado=true; v.motivoAnulacion=motivo; v.anuladoPor=u.id; v.anuladoEn=Instant.now(); pesajes.save(v)
    }
    fun omitir(rutaId: Long,puntoId: Long,motivo: String): Map<String,Any> {
        exigir("PESAJE_CREAR"); val r=bloquearRuta(rutaId); propia(r); ReglasPesaje.rutaAbierta(r.cerrada)
        val rp=asignaciones.findByRutaIdAndPuntoId(rutaId,puntoId) ?: throw ErrorNegocio(404,"Punto no asignado")
        rp.motivoOmision=motivo; asignaciones.saveAndFlush(rp); return rutaDto(r)
    }
    fun cerrar(id: Long): Map<String,Any> {
        exigir("RUTA_CERRAR"); val r=bloquearRuta(id); propia(r); ReglasPesaje.rutaAbierta(r.cerrada)
        ReglasPesaje.puedeCerrar(asignaciones.findByRutaIdOrderBySecuencia(id).count { it.motivoOmision.isNullOrBlank() && !pesajes.existsByRutaIdAndPuntoIdAndAnuladoFalse(id,it.puntoId) })
        r.cerrada=true; rutas.saveAndFlush(r); return rutaDto(r)
    }
}
