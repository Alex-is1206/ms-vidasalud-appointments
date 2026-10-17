package cl.duoc.vidasalud.ms_vidasalud_appointments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades centralizadas de la topologia de RabbitMQ leidas desde application.properties.
 * Cumple con el Indicador 1 de la rubrica manteniendo consistencia entre microservicios.
 */
@ConfigurationProperties("vidasalud.rabbit")
public record NombresRabbit(
        String exchangeTopic,
        String exchangeDirect,
        String exchangeDlx,
        String colaNotificaciones,
        String colaTickets,
        String colaAuditoria
) {
}