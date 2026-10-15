package cl.duoc.vidasalud.ms_vidasalud_appointments.evento;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Sobre comun de eventos de VidaSalud.
 *
 * Es exactamente la misma estructura que consume ms-vidasalud-notify, para que
 * productor y consumidor hablen el mismo contrato. El timestamp viaja como texto
 * ISO-8601 para no depender de modulos extra de serializacion de fechas.
 */
public record EventoVidaSalud(
        String type,
        String eventId,
        String timestamp,
        String traceId,
        String correlationId,
        Map<String, Object> payload) {

    /**
     * Construye un evento con los metadatos de trazabilidad ya resueltos.
     *
     * @param type    nombre del evento de negocio, por ejemplo atencion.confirmada
     * @param traceId identificador para seguir la operacion de punta a punta
     * @param payload datos propios del evento
     */
    public static EventoVidaSalud de(String type, String traceId, Map<String, Object> payload) {
        String id = UUID.randomUUID().toString();
        return new EventoVidaSalud(
                type,
                id,
                Instant.now().toString(),
                traceId != null ? traceId : id,
                id,
                payload);
    }
}
