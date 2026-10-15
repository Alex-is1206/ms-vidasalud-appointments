package cl.duoc.vidasalud.ms_vidasalud_appointments.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion del lado productor del catalogo.
 *
 * El catalogo publica en un exchange de tipo direct porque sus eventos tienen
 * un unico destino conocido: la cola de auditoria. No necesita repartir por
 * patron como si ocurre con las atenciones.
 */
@Configuration
public class RabbitProductorConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitProductorConfig.class);

    /** Exchange de tipo direct para los cambios de catalogo. */
    public static final String EXCHANGE_DIRECT = "cmd.direct";

    /** Routing key exacta que escucha la cola de auditoria. */
    public static final String RK_CATALOGO_CAMBIO = "catalogo.cambio";

    @Bean
    DirectExchange exchangeCatalogo() {
        return new DirectExchange(EXCHANGE_DIRECT, true, false);
    }

    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    MessageConverter conversorJson() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter conversorJson) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(conversorJson);
        template.setMandatory(true);

        template.setConfirmCallback((correlacion, aceptado, motivo) -> {
            if (!aceptado) {
                log.error("el broker rechazo el mensaje {}: {}",
                        correlacion != null ? correlacion.getId() : "sin-id", motivo);
            }
        });

        template.setReturnsCallback(devuelto -> log.error(
                "mensaje sin cola destino | exchange {} | routing key {} | motivo {}",
                devuelto.getExchange(), devuelto.getRoutingKey(), devuelto.getReplyText()));

        return template;
    }
}
