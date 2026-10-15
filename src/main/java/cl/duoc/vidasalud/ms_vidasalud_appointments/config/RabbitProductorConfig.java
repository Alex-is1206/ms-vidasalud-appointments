package cl.duoc.vidasalud.ms_vidasalud_appointments.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion del lado productor de atenciones.
 *
 * Este microservicio solo publica, por lo tanto declara unicamente el exchange
 * de tipo topic al que escribe. Las colas y las DLQ las declara el consumidor
 * (ms-vidasalud-notify), que es el dueno de su propia topologia.
 */
@Configuration
public class RabbitProductorConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitProductorConfig.class);

    /** Exchange de tipo topic para los eventos de atenciones. */
    public static final String EXCHANGE_TOPIC = "cmd.topic";

    /** Routing key que se emite cuando una atencion queda confirmada. */
    public static final String RK_ATENCION_CONFIRMADA = "atencion.confirmada";

    /** Routing key que se emite cuando una atencion se cancela. */
    public static final String RK_ATENCION_CANCELADA = "atencion.cancelada";

    @Bean
    TopicExchange exchangeAtenciones() {
        return new TopicExchange(EXCHANGE_TOPIC, true, false);
    }

    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    MessageConverter conversorJson() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * RabbitTemplate con confirmaciones del broker activadas.
     *
     * El confirm callback avisa si el broker no acepto el mensaje y el returns
     * callback avisa si el mensaje fue aceptado pero ninguna cola lo recibio,
     * lo que normalmente significa que falta un binding.
     */
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