package cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres y conexion de Kafka leidos desde application.properties (prefijo vidasalud.kafka).
 *
 * Es el mismo record en todos los microservicios que usan Kafka, igual que NombresRabbit:
 * los nombres de los topicos viven en un solo lugar y se pueden cambiar por entorno
 * sin recompilar. La configuracion de cada topico (particiones, replicas, retencion)
 * no esta aqui: la administra ms-vidasalud-kafka-admin.
 *
 * @param bootstrapServers  lista de brokers a los que se conecta el cliente
 * @param clientId          identificador del cliente, visible en los logs de los brokers
 * @param topicoAtenciones  topico con el ciclo de vida de las atenciones
 * @param topicoBoxes       topico compactado con el ultimo estado de cada box
 */
@ConfigurationProperties("vidasalud.kafka")
public record NombresKafka(
        String bootstrapServers,
        String clientId,
        String topicoAtenciones,
        String topicoBoxes
) {
}
