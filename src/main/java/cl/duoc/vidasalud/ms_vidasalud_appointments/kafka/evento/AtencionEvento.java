package cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.evento;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Evento de streaming publicado en el topico vidasalud.atenciones.eventos.
 *
 * Es el contrato entre ms-vidasalud-appointments (productor) y ms-vidasalud-analytics
 * (consumidor): ambos tienen este mismo record. Cada campo existe por un caso de uso:
 *
 * eventId            identifica el evento: idempotencia en el consumidor y trazabilidad.
 * tipo               que paso: creacion o cambio de estado.
 * ocurridoEn         cuando paso (ISO-8601): metricas por minuto y orden del historial.
 * atencionId         clave del mensaje en Kafka: todos los eventos de una atencion caen
 *                    en la misma particion y se consumen en orden.
 * estadoAnterior     de donde venia (null al crearla): permite restar del contador anterior.
 * estadoNuevo        a donde llego: permite sumar al contador nuevo.
 * prestacionId       metricas por tipo de prestacion.
 * boxId              ocupacion por box (puede ser null si aun no se asigna).
 * fechaHoraAtencion  para cuando esta agendada la atencion (ISO-8601).
 * origen             que microservicio emitio el evento.
 * version            version del esquema, para evolucionarlo sin romper consumidores.
 *
 * El evento no lleva datos personales del paciente: la analitica no los necesita.
 * Las fechas viajan como texto ISO-8601 para no depender de modulos de serializacion de fechas.
 */
public record AtencionEvento(
        @NotBlank String eventId,
        @NotNull TipoEventoAtencion tipo,
        @NotBlank String ocurridoEn,
        @NotNull @Positive Long atencionId,
        String estadoAnterior,
        @NotBlank String estadoNuevo,
        @NotNull Long prestacionId,
        Long boxId,
        String fechaHoraAtencion,
        @NotBlank String origen,
        int version
) {

    /** Version actual del esquema del evento. */
    public static final int VERSION_ACTUAL = 1;

    /** Microservicio que emite este evento. */
    public static final String ORIGEN = "ms-vidasalud-appointments";

    /**
     * Construye un evento nuevo con identificador y marca de tiempo ya resueltos.
     */
    public static AtencionEvento de(TipoEventoAtencion tipo,
                                    Long atencionId,
                                    String estadoAnterior,
                                    String estadoNuevo,
                                    Long prestacionId,
                                    Long boxId,
                                    String fechaHoraAtencion) {
        return new AtencionEvento(
                UUID.randomUUID().toString(),
                tipo,
                Instant.now().toString(),
                atencionId,
                estadoAnterior,
                estadoNuevo,
                prestacionId,
                boxId,
                fechaHoraAtencion,
                ORIGEN,
                VERSION_ACTUAL);
    }
}
