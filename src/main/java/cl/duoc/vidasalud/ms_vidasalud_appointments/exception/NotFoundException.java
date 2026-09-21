package cl.duoc.vidasalud.ms_vidasalud_appointments.exception;

/** El recurso solicitado no existe: se traduce a HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
