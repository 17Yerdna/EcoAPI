package pe.upeu.ecorecicla

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import tools.jackson.databind.ObjectMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"])
class ApiIntegrationTest {
    @Value("\${local.server.port}") var port: Int = 0
    @Autowired lateinit var db: JdbcTemplate
    @Autowired lateinit var em: jakarta.persistence.EntityManager
    @Autowired lateinit var txManager: org.springframework.transaction.PlatformTransactionManager
    @Autowired lateinit var dataSource: javax.sql.DataSource
    @Autowired lateinit var service: pe.upeu.ecorecicla.servicio.EcoService
    val http = HttpClient.newHttpClient()
    val json = ObjectMapper()
    fun request(method: String, path: String, body: String = "", token: String? = null): HttpResponse<String> {
        val b = HttpRequest.newBuilder(URI("http://localhost:$port/api/v1$path")).header("Content-Type","application/json")
        if (token != null) b.header("Authorization","Bearer $token")
        return http.send(b.method(method,HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString())
    }
    fun login(user: String): String {
        val r=request("POST","/auth/login","""{"usuario":"$user","contrasena":"Eco2026*"}""")
        assertEquals(200,r.statusCode(),r.body())
        return json.readTree(r.body()).get("token").asText()
    }
    @Test fun `semillas relacionales completas`() {
        assertEquals(12,db.queryForObject("select count(*) from punto_acopio",Int::class.java))
        assertEquals(2,db.queryForObject("select count(*) from ruta",Int::class.java))
        assertTrue(db.queryForObject("select count(*) from pesaje",Int::class.java)!! >= 15)
    }
    @Test fun `login firma token y rutas exigen autenticacion`() {
        assertEquals(401,request("GET","/rutas/dia").statusCode())
        assertEquals(401,request("GET","/rutas/dia",token="alterado").statusCode())
        assertEquals(401,request("POST","/auth/login","""{"usuario":"operario1","contrasena":"mala"}""").statusCode())
        val r=request("GET","/rutas/dia",token=login("operario1"))
        assertEquals(200,r.statusCode(),r.body())
        assertEquals(6,json.readTree(r.body()).get("puntos").size())
    }
    @Test fun `admin no registra pesajes y error mantiene contrato`() {
        val r=request("POST","/pesajes","""{"puntoId":1,"material":"PAPEL","pesoBruto":10,"tara":2}""",login("admin"))
        assertEquals(403,r.statusCode(),r.body())
        for (field in listOf("timestamp","status","error","message","path","validationErrors")) assertTrue(json.readTree(r.body()).has(field))
    }
    @Test fun `RN03 validacion y RN04 conflicto se traducen a HTTP`() {
        val op=login("operario1")
        val r=request("POST","/pesajes","""{"puntoId":1,"material":"PAPEL","pesoBruto":2,"tara":3}""",op)
        assertEquals(400,r.statusCode(),r.body())
        assertTrue(json.readTree(r.body()).get("validationErrors").has("pesoBruto"))
        assertEquals(409,request("POST","/rutas/1/cierre",token=login("supervisor1")).statusCode())
    }
    @Test fun `RN01 ruta cerrada y RN02 pesaje ajeno son rechazados`() {
        val token=login("operario1")
        val b="""{"material":"PAPEL","pesoBruto":12,"tara":2}"""
        assertEquals(409,request("PUT","/pesajes/2",b,token).statusCode())
        assertEquals(403,request("PUT","/pesajes/15",b,token).statusCode())
        assertEquals(200,request("PUT","/pesajes/11",b,token).statusCode())
    }
    @Test fun `alta idempotente y anulacion logica devuelve 204`() {
        val token=login("operario1")
        val key=java.util.UUID.randomUUID().toString()
        val b="""{"puntoId":1,"material":"PAPEL","pesoBruto":12,"tara":2,"claveCliente":"$key"}"""
        val primero=request("POST","/pesajes",b,token)
        assertEquals(201,primero.statusCode(),primero.body())
        val id=json.readTree(primero.body()).get("id").asLong()
        assertEquals(id,json.readTree(request("POST","/pesajes",b,token).body()).get("id").asLong())
        assertEquals(403,request("DELETE","/pesajes/$id?motivo=Error",token=token).statusCode())
        val anular=request("DELETE","/pesajes/$id?motivo=Error",token=login("supervisor1"))
        assertEquals(204,anular.statusCode(),anular.body())
        assertTrue(anular.body().isEmpty())
        assertEquals(true,db.queryForObject("select anulado from pesaje where id=?",Boolean::class.java,id))
    }
    @Test fun `gestion de puntos exige privilegio y paginacion respeta wrapper`() {
        val b="""{"razonSocial":"Prueba","direccion":"Av. 1","zona":"Centro","materialHabitual":"PAPEL"}"""
        assertEquals(403,request("POST","/puntos",b,login("operario1")).statusCode())
        val r=request("GET","/puntos?pagina=0&tamanio=2",token=login("admin"))
        assertEquals(200,r.statusCode(),r.body())
        val j=json.readTree(r.body())
        for(f in listOf("contenido","pagina","tamanio","totalElementos","totalPaginas","ultima")) assertTrue(j.has(f))
        assertEquals(2,j.get("contenido").size())
        assertEquals(404,request("GET","/puntos/99999/pesajes",token=login("admin")).statusCode())
    }
    @Test fun `RN01 refresca una ruta cargada antes del cierre concurrente`() {
        val p=pe.upeu.ecorecicla.seguridad.Principal(1,"operario1","OPERARIO",setOf("PESAJE_CREAR"))
        org.springframework.security.core.context.SecurityContextHolder.getContext().authentication=org.springframework.security.authentication.UsernamePasswordAuthenticationToken(p,null,emptyList())
        try {
            val e=assertThrows(pe.upeu.ecorecicla.servicio.ErrorNegocio::class.java) {
                org.springframework.transaction.support.TransactionTemplate(txManager).execute {
                    em.find(pe.upeu.ecorecicla.dominio.Ruta::class.java,1L)
                    dataSource.connection.use { c -> c.createStatement().use { it.executeUpdate("update ruta set cerrada=true where id=1") } }
                    service.registrar(pe.upeu.ecorecicla.web.dto.PesajeRequest(puntoId=1,material="PAPEL",pesoBruto=10.0,tara=2.0))
                }
            }
            assertEquals(409,e.status)
        } finally {
            db.update("update ruta set cerrada=false where id=1")
            org.springframework.security.core.context.SecurityContextHolder.clearContext()
        }
    }
}
