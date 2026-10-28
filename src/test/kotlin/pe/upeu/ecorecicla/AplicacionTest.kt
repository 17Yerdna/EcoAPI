package pe.upeu.ecorecicla

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull

class AplicacionTest {
    @Test fun `existe el punto de arranque de Spring Boot`() {
        assertNotNull(Class.forName("pe.upeu.ecorecicla.Aplicacion"))
    }
}
