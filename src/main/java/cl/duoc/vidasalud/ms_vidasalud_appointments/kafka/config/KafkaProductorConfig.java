package cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.evento.AtencionEvento;

/**
 * Configuracion del cliente Kafka del lado productor.
 *
 * Toda la configuracion de mensajeria vive aqui: el servicio de negocio no conoce
 * Kafka, solo llama a PublicadorAtencionesKafka. Los topicos no se declaran en este
 * microservicio: los crea ms-vidasalud-kafka-admin con sus particiones, replicas y
 * politicas de retencion.
 */
@Configuration
public class KafkaProductorConfig {

    private final NombresKafka nombres;

    public KafkaProductorConfig(NombresKafka nombres) {
        this.nombres = nombres;
    }

    /**
     * Fabrica de productores con garantias de entrega fuertes.
     *
     * - acks=all + enable.idempotence=true: la escritura se confirma solo cuando esta en
     *   todas las replicas sincronizadas (min.insync.replicas=2 en el topico) y los
     *   reintentos nunca duplican mensajes.
     * - max.block.ms=3000: si Kafka no responde, el envio falla en 3 segundos en vez de
     *   dejar colgada la peticion HTTP del usuario durante 60 (valor por defecto).
     * - linger.ms + compresion lz4: agrupa mensajes cercanos en un solo envio.
     * - Sin cabeceras de tipo: cada microservicio tiene su propio paquete Java, asi que el
     *   consumidor deserializa al DTO que el mismo define, no al nombre de clase del productor.
     */
    @Bean
    public ProducerFactory<String, AtencionEvento> productorAtencionesFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, nombres.bootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, nombres.clientId());

        // Serializacion: clave como texto (atencionId) y valor como JSON del DTO
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put("spring.json.add.type.headers", false);

        // Durabilidad y sin duplicados
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Tiempos: no bloquear la API si el cluster no esta disponible
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);

        // Rendimiento
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, AtencionEvento> kafkaTemplateAtenciones(
            ProducerFactory<String, AtencionEvento> productorAtencionesFactory) {
        KafkaTemplate<String, AtencionEvento> template = new KafkaTemplate<>(productorAtencionesFactory);
        template.setDefaultTopic(nombres.topicoAtenciones());
        return template;
    }
}
