package pe.upeu.ecorecicla.seguridad

import jakarta.persistence.*

@Entity @Table(name="usuario")
open class Usuario(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) open var id: Long = 0,
    @Column(unique=true) open var usuario: String = "",
    open var contrasena: String = "",
    open var rol: String = ""
)

data class Principal(val id: Long, val usuario: String, val rol: String, val privilegios: Set<String>)
