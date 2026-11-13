package cl.duoc.vidasalud.ms_vidasalud_appointments.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.config.NombresKafka;
import cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.evento.AtencionEvento;
import cl.duoc.vidasalud.ms_vidasalud_appointments.kafka.evento.TipoEventoAtencion;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.Appointment;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.AppointmentStatus;

/**
 * Pruebas del productor Kafka sin broker real: se simula el KafkaTemplate y se
 * revisa el topico, la clave y el contenido del DTO que se envia.
 */
@ExtendWith(MockitoExtension.class)
class PublicadorAtencionesKafkaTest {

    private static final String TOPICO = "vidasalud.atenciones.eventos";

    @Mock
    private KafkaTemplate<String, AtencionEvento> kafkaTemplate;

    private PublicadorAtencionesKafka publicador;

    @BeforeEach
    void setUp() {
        NombresKafka nombres = new NombresKafka("localhost:9092", "test", TOPICO, "vidasalud.boxes.estado");
        publicador = new PublicadorAtencionesKafka(kafkaTemplate, nombres);
    }

    private Appointment atencion(AppointmentStatus estado) {
        Appointment a = new Appointment();
        a.setId(21L);
        a.setPacienteEmail("paciente@vidasalud.cl");
        a.setPrestacionId(3L);
        a.setBoxId(7L);
        a.setFechaHora(LocalDateTime.of(2026, 11, 20, 9, 0));
        a.setEstado(estado);
        return a;
    }

    @Test
    @DisplayName("Cambio de estado: se envia al topico de atenciones con clave atencionId y DTO completo")
    void estadoCambiadoEnviaDtoConClave() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AtencionEvento.class)))
                .thenReturn(new CompletableFuture<SendResult<String, AtencionEvento>>());

        publicador.estadoCambiado(atencion(AppointmentStatus.CONFIRMADA), AppointmentStatus.SOLICITADA);

        ArgumentCaptor<AtencionEvento> evento = ArgumentCaptor.forClass(AtencionEvento.class);
        verify(kafkaTemplate).send(eq(TOPICO), eq("21"), evento.capture());

        AtencionEvento enviado = evento.getValue();
        assertThat(enviado.tipo()).isEqualTo(TipoEventoAtencion.ESTADO_CAMBIADO);
        assertThat(enviado.estadoAnterior()).isEqualTo("SOLICITADA");
        assertThat(enviado.estadoNuevo()).isEqualTo("CONFIRMADA");
        assertThat(enviado.atencionId()).isEqualTo(21L);
        assertThat(enviado.boxId()).isEqualTo(7L);
        assertThat(enviado.eventId()).isNotBlank();
        assertThat(enviado.origen()).isEqualTo(AtencionEvento.ORIGEN);
        assertThat(enviado.version()).isEqualTo(AtencionEvento.VERSION_ACTUAL);
    }

    @Test
    @DisplayName("Creacion: el evento no tiene estado anterior y no lleva datos personales")
    void atencionCreadaSinEstadoAnterior() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AtencionEvento.class)))
                .thenReturn(new CompletableFuture<SendResult<String, AtencionEvento>>());

        publicador.atencionCreada(atencion(AppointmentStatus.SOLICITADA));

        ArgumentCaptor<AtencionEvento> evento = ArgumentCaptor.forClass(AtencionEvento.class);
        verify(kafkaTemplate).send(eq(TOPICO), eq("21"), evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo(TipoEventoAtencion.ATENCION_CREADA);
        assertThat(evento.getValue().estadoAnterior()).isNull();
        assertThat(evento.getValue().toString()).doesNotContain("paciente@vidasalud.cl");
    }

    @Test
    @DisplayName("Si Kafka falla al enviar, el error se registra y no se propaga al negocio")
    void falloSincronicoNoSePropaga() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AtencionEvento.class)))
                .thenThrow(new RuntimeException("cluster no disponible"));

        assertThatCode(() -> publicador.atencionCreada(atencion(AppointmentStatus.SOLICITADA)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Si el envio falla de forma asincrona, tampoco se propaga")
    void falloAsincronoNoSePropaga() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AtencionEvento.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("timeout")));

        assertThatCode(() -> publicador.estadoCambiado(atencion(AppointmentStatus.CANCELADA), AppointmentStatus.CONFIRMADA))
                .doesNotThrowAnyException();
    }
}
