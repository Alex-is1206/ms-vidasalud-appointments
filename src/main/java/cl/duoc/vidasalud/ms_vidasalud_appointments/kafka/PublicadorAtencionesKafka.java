package cl.duoc.vidasalud.ms_vidasalud_appointments.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.config.NombresKafka;
import cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.evento.AtencionEvento;
import cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.evento.TipoEventoAtencion;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.Appointment;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.AppointmentStatus;

/**
 * Publica el ciclo de vida de las atenciones en el topico de streaming.
 *
 * Traduce la entidad a un AtencionEvento y lo envia con clave = atencionId, de modo que
 * todos los eventos de una misma atencion queden en la misma particion y en orden.
 *
 * Igual que la publicacion en RabbitMQ, nunca interrumpe la operacion de negocio:
 * la atencion ya quedo guardada en Oracle y un fallo de Kafka solo se registra en el log.
 */
@Component
public class PublicadorAtencionesKafka {

    private static final Logger log = LoggerFactory.getLogger(PublicadorAtencionesKafka.class);

    private final KafkaTemplate<String, AtencionEvento> kafkaTemplate;
    private final NombresKafka nombres;

    public PublicadorAtencionesKafka(KafkaTemplate<String, AtencionEvento> kafkaTemplate,
                                     NombresKafka nombres) {
        this.kafkaTemplate = kafkaTemplate;
        this.nombres = nombres;
    }

    /** Publica que se registro una atencion nueva. */
    public void atencionCreada(Appointment atencion) {
        publicar(AtencionEvento.de(
                TipoEventoAtencion.ATENCION_CREADA,
                atencion.getId(),
                null,
                nombreEstado(atencion.getEstado()),
                atencion.getPrestacionId(),
                atencion.getBoxId(),
                atencion.getFechaHora() != null ? atencion.getFechaHora().toString() : null));
    }

    /** Publica una transicion de estado de una atencion existente. */
    public void estadoCambiado(Appointment atencion, AppointmentStatus estadoAnterior) {
        publicar(AtencionEvento.de(
                TipoEventoAtencion.ESTADO_CAMBIADO,
                atencion.getId(),
                nombreEstado(estadoAnterior),
                nombreEstado(atencion.getEstado()),
                atencion.getPrestacionId(),
                atencion.getBoxId(),
                atencion.getFechaHora() != null ? atencion.getFechaHora().toString() : null));
    }

    private void publicar(AtencionEvento evento) {
        String topico = nombres.topicoAtenciones();
        String clave = String.valueOf(evento.atencionId());

        try {
            kafkaTemplate.send(topico, clave, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("kafka | no se pudo publicar | {} | atencion {} | eventId {} | motivo {}",
                            evento.tipo(), evento.atencionId(), evento.eventId(), error.getMessage());
                    return;
                }
                RecordMetadata meta = resultado.getRecordMetadata();
                log.info("kafka | evento publicado | {} {} -> {} | atencion {} | topico {} | particion {} | offset {}",
                        evento.tipo(), evento.estadoAnterior(), evento.estadoNuevo(), evento.atencionId(),
                        meta.topic(), meta.partition(), meta.offset());
            });
        } catch (Exception e) {
            // Errores sincronicos: serializacion o cluster sin respuesta dentro de max.block.ms
            log.error("kafka | no se pudo enviar | {} | atencion {} | eventId {} | motivo {}",
                    evento.tipo(), evento.atencionId(), evento.eventId(), e.getMessage());
        }
    }

    private String nombreEstado(AppointmentStatus estado) {
        return estado != null ? estado.name() : null;
    }
}
