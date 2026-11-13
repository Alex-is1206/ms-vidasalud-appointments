package cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.evento;

/**
 * Tipos de evento del ciclo de vida de una atencion.
 *
 * ATENCION_CREADA se emite una vez, al registrar la atencion.
 * ESTADO_CAMBIADO se emite en cada transicion (confirmada, en espera, en atencion,
 * cerrada o cancelada); el detalle va en estadoAnterior y estadoNuevo.
 */
public enum TipoEventoAtencion {
    ATENCION_CREADA,
    ESTADO_CAMBIADO
}
