package pe.upeu.ecorecicla

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import pe.upeu.ecorecicla.config.JwtService
import pe.upeu.ecorecicla.seguridad.Principal
import java.util.Date

class JwtServiceTest {
    private val secret="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val jwt=JwtService(secret)
    @Test fun `firma valida conserva usuario y privilegios`() {
        val p=Principal(1,"operario1","OPERARIO",setOf("PUNTO_LEER"))
        assertEquals(p,jwt.validar(jwt.generar(p)))
    }
    @Test fun `token vencido no se acepta`() {
        val t=Jwts.builder().subject("1").expiration(Date(System.currentTimeMillis()-60000)).signWith(Keys.hmacShaKeyFor(secret.toByteArray())).compact()
        assertThrows(ExpiredJwtException::class.java) {jwt.validar(t)}
    }
    @Test fun `firma alterada no se acepta`() {
        val t=jwt.generar(Principal(1,"operario1","OPERARIO",setOf("PUNTO_LEER")))
        val parts=t.split('.').toMutableList(); val signature=parts[2].toCharArray(); signature[0]=if(signature[0]=='A') 'B' else 'A'; parts[2]=String(signature)
        assertThrows(JwtException::class.java) {jwt.validar(parts.joinToString("."))}
    }
}
