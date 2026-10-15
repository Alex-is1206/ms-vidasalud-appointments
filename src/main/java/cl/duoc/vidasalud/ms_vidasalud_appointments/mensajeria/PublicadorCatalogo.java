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
 * Publica en el exchange direct cada cambio del catalogo, para que quede
 * registrado en la cola de auditoria.
 *
 * Igual que en atenciones, un fallo de mensajeria se registra pero no revierte
 * ni interrumpe el cambio que ya se guardo en la base de datos.
 */
@Component
public class PublicadorCatalogo {

    private static final Logger log = LoggerFactory.getLogger(PublicadorCatalogo.class);

    private final RabbitTemplate rabbitTemplate;

    public PublicadorCatalogo(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Registra un cambio sobre una entidad del catalogo.
     *
     * @param entidad  tipo de elemento afectado, por ejemplo especialidad o box
     * @param accion   operacion realizada: CREADO, ACTUALIZADO o ELIMINADO
     * @param recursoId identificador del elemento afectado
     * @param usuario  correo del usuario que ejecuto el cambio
     */
    public void cambioRegistrado(String entidad, String accion, Object recursoId, String usuario) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("entidad", entidad);
        datos.put("accion", accion);
        datos.put("recursoId", String.valueOf(recursoId));
        datos.put("usuario", usuario != null ? usuario : "desconocido");

        EventoVidaSalud evento = EventoVidaSalud.de("catalogo.cambio", null, datos);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitProductorConfig.EXCHANGE_DIRECT,
                    RabbitProductorConfig.RK_CATALOGO_CAMBIO,
                    evento,
                    new CorrelationData(evento.eventId()));

            log.info("evento publicado | catalogo.cambio | {} {} id {} | eventId {}",
                    entidad, accion, recursoId, evento.eventId());

        } catch (Exception e) {
            log.error("no se pudo publicar el cambio de catalogo {} {}: {}",
                    entidad, accion, e.getMessage());
        }
    }
}
