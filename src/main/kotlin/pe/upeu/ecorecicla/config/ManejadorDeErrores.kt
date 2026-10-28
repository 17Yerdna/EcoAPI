package pe.upeu.ecorecicla.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.dao.DataIntegrityViolationException
import pe.upeu.ecorecicla.servicio.ErrorNegocio
import pe.upeu.ecorecicla.web.dto.ErrorResponse

@RestControllerAdvice
class ManejadorDeErrores {
    @ExceptionHandler(ErrorNegocio::class)
    fun negocio(e: ErrorNegocio,r: HttpServletRequest) = response(e.status,e.message,r.requestURI,e.campos)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(e: MethodArgumentNotValidException,r: HttpServletRequest) = response(400,"Revise los campos",r.requestURI,e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Inválido") })
    @ExceptionHandler(HttpMessageNotReadableException::class,IllegalArgumentException::class)
    fun formato(e: Exception,r: HttpServletRequest) = response(400,"Solicitud inválida",r.requestURI)
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun conflicto(e: Exception,r: HttpServletRequest) = response(409,"Los datos entran en conflicto con un registro existente",r.requestURI)
    @ExceptionHandler(Exception::class)
    fun servidor(e: Exception,r: HttpServletRequest): ResponseEntity<ErrorResponse> {
        org.slf4j.LoggerFactory.getLogger(javaClass).error("Fallo interno en {}",r.requestURI,e)
        return response(500,"Error interno. Intente nuevamente",r.requestURI)
    }
    private fun response(s: Int,m: String,p: String,c: Map<String,String> = emptyMap()) = ResponseEntity.status(s).body(ErrorResponse(status=s,error=HttpStatus.valueOf(s).reasonPhrase,message=m,path=p,validationErrors=c))
}
