package pe.upeu.ecorecicla.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import pe.upeu.ecorecicla.seguridad.Principal
import pe.upeu.ecorecicla.web.dto.ErrorResponse
import java.util.Date

@Component
class JwtService(@Value("\${jwt.secret}") secret: String) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray().also { require(it.size>=64) { "JWT_SECRET requiere al menos 64 bytes" } })
    fun generar(p: Principal): String = Jwts.builder().subject(p.id.toString()).claim("usuario",p.usuario).claim("rol",p.rol).claim("privilegios",p.privilegios).issuedAt(Date()).expiration(Date(System.currentTimeMillis()+3600000)).signWith(key).compact()
    fun validar(token: String): Principal {
        val c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        return Principal(c.subject.toLong(),c["usuario"].toString(),c["rol"].toString(),(c["privilegios"] as List<*>).map { it.toString() }.toSet())
    }
}
@Component
class JsonSeguridad(private val json: ObjectMapper) {
    fun error(r: HttpServletRequest,s: HttpServletResponse,status: Int) {
        s.status=status; s.contentType="application/json"; s.characterEncoding="UTF-8"
        s.writer.write(json.writeValueAsString(ErrorResponse(status=status,error=if(status==401) "Unauthorized" else "Forbidden",message=if(status==401) "Sesión ausente, inválida o vencida" else "No tiene el privilegio requerido",path=r.requestURI)))
    }
}
@Component
class JwtFilter(private val jwt: JwtService,private val errors: JsonSeguridad) : OncePerRequestFilter() {
    override fun doFilterInternal(r: HttpServletRequest,s: HttpServletResponse,chain: FilterChain) {
        val h=r.getHeader("Authorization")
        if (h!=null && !r.requestURI.endsWith("/auth/login")) {
            val p=try { require(h.startsWith("Bearer ")); jwt.validar(h.substring(7)) } catch(e: Exception) { errors.error(r,s,401); return }
            SecurityContextHolder.getContext().authentication=UsernamePasswordAuthenticationToken(p,null,p.privilegios.map(::SimpleGrantedAuthority))
        }
        chain.doFilter(r,s)
    }
}
@Configuration
class SecurityConfig {
    @Bean fun passwordEncoder() = BCryptPasswordEncoder()
    @Bean fun security(http: HttpSecurity,filter: JwtFilter,errors: JsonSeguridad): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests { it.requestMatchers("/api/v1/auth/login","/actuator/health","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll().anyRequest().authenticated() }
        .exceptionHandling { it.authenticationEntryPoint { r,s,_ -> errors.error(r,s,401) }.accessDeniedHandler { r,s,_ -> errors.error(r,s,403) } }
        .addFilterBefore(filter,UsernamePasswordAuthenticationFilter::class.java).build()
}
