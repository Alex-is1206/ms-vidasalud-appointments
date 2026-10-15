package cl.duoc.vidasalud.ms_vidasalud_appointments.mensajeria;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.ms_vidasalud_appointments.config.RabbitProductorConfig;
import cl.duoc.vidasalud.ms_vidasalud_appointments.evento.EventoVidaSalud;

/**
 * Publica los eventos de atenciones en el exchange topic.
 *
 * La publicacion nunca interrumpe la operacion de negocio: si RabbitMQ no esta
 * disponible, la atencion ya quedo guardada en la base de datos y el error solo
 * se registra en el log.
 */
@Component
public class PublicadorAtenciones {

    private static final Logger log = LoggerFactory.getLogger(PublicadorAtenciones.class);

    private final RabbitTemplate rabbitTemplate;

    public PublicadorAtenciones(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Avisa que una atencion quedo confirmada.
     *
     * @param atencionId    identificador de la atencion
     * @param pacienteEmail correo del paciente al que hay que notificar
     * @param boxId         box asignado para la atencion
     */
    public void atencionConfirmada(Object atencionId, String pacienteEmail, Object boxId) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("atencionId", String.valueOf(atencionId));
        datos.put("pacienteEmail", pacienteEmail);
        datos.put("estado", "CONFIRMADA");
        datos.put("boxId", String.valueOf(boxId));

        publicar(RabbitProductorConfig.RK_ATENCION_CONFIRMADA,
                EventoVidaSalud.de("atencion.confirmada", null, datos));
    }

    /**
     * Avisa que una atencion fue cancelada.
     *
     * @param atencionId    identificador de la atencion
     * @param pacienteEmail correo del paciente al que hay que notificar
     * @param motivo        razon de la cancelacion
     */
    public void atencionCancelada(Object atencionId, String pacienteEmail, String motivo) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("atencionId", String.valueOf(atencionId));
        datos.put("pacienteEmail", pacienteEmail);
        datos.put("estado", "CANCELADA");
        datos.put("motivo", motivo != null ? motivo : "sin detalle");

        publicar(RabbitProductorConfig.RK_ATENCION_CANCELADA,
                EventoVidaSalud.de("atencion.cancelada", null, datos));
    }

    private void publicar(String routingKey, EventoVidaSalud evento) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitProductorConfig.EXCHANGE_TOPIC,
                    routingKey,
                    evento,
                    new CorrelationData(evento.eventId()));

            log.info("evento publicado | {} | routing key {} | eventId {}",
                    evento.type(), routingKey, evento.eventId());

        } catch (Exception e) {
            log.error("no se pudo publicar el evento {} con eventId {}: {}",
                    evento.type(), evento.eventId(), e.getMessage());
        }
    }
}
