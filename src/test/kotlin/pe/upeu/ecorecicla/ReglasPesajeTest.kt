package pe.upeu.ecorecicla

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pe.upeu.ecorecicla.servicio.ReglasPesaje
import pe.upeu.ecorecicla.servicio.ErrorNegocio
import java.time.LocalDate

class ReglasPesajeTest {
    @Test fun `RN03 calcula neto y rechaza neto no positivo`() {
        assertEquals(8.0, ReglasPesaje.neto(10.0, 2.0))
        assertEquals(400, assertThrows(ErrorNegocio::class.java) { ReglasPesaje.neto(2.0, 2.0) }.status)
        assertThrows(ErrorNegocio::class.java) { ReglasPesaje.neto(Double.NaN, 1.0) }
    }
    @Test fun `RN01 impide modificar ruta cerrada`() {
        assertEquals(409, assertThrows(ErrorNegocio::class.java) { ReglasPesaje.rutaAbierta(true) }.status)
    }
    @Test fun `RN02 exige propio y mismo dia sin privilegio global`() {
        val hoy = LocalDate.of(2026,10,28)
        ReglasPesaje.puedeCorregir(1,1,hoy,hoy,false)
        assertEquals(403, assertThrows(ErrorNegocio::class.java) { ReglasPesaje.puedeCorregir(1,2,hoy,hoy,false) }.status)
        assertEquals(403, assertThrows(ErrorNegocio::class.java) { ReglasPesaje.puedeCorregir(1,1,hoy.minusDays(1),hoy,false) }.status)
        ReglasPesaje.puedeCorregir(1,2,hoy.minusDays(1),hoy,true)
    }
    @Test fun `RN04 informa cantidad de puntos pendientes`() {
        assertTrue(assertThrows(ErrorNegocio::class.java) { ReglasPesaje.puedeCerrar(3) }.message!!.contains("3"))
        ReglasPesaje.puedeCerrar(0)
    }
}
