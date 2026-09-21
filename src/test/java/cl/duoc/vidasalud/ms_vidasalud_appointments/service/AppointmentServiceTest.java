package cl.duoc.vidasalud.ms_vidasalud_appointments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.vidasalud.ms_vidasalud_appointments.exception.NotFoundException;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.Appointment;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.AppointmentStatus;
import cl.duoc.vidasalud.ms_vidasalud_appointments.repository.AppointmentRepository;

/**
 * Pruebas de la lógica de atenciones. El repositorio se simula con
 * Mockito, así que no necesitan base de datos.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository repository;

    @InjectMocks
    private AppointmentService service;

    private Appointment atencionEn(AppointmentStatus estado) {
        Appointment a = new Appointment();
        a.setId(1L);
        a.setPacienteEmail("paciente@vidasalud.cl");
        a.setPrestacionId(1L);
        a.setFechaHora(LocalDateTime.of(2026, 10, 1, 10, 30));
        a.setEstado(estado);
        return a;
    }

    @Test
    @DisplayName("Toda atención nueva parte en SOLICITADA")
    void crearAsignaEstadoSolicitada() {
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment nueva = atencionEn(AppointmentStatus.CERRADA);
        Appointment creada = service.crear(nueva);

        assertThat(creada.getEstado()).isEqualTo(AppointmentStatus.SOLICITADA);
        assertThat(creada.getId()).isNull();
    }

    @Test
    @DisplayName("Transición válida: SOLICITADA -> CONFIRMADA")
    void transicionValida() {
        when(repository.findById(1L)).thenReturn(Optional.of(atencionEn(AppointmentStatus.SOLICITADA)));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment resultado = service.cambiarEstado(1L, AppointmentStatus.CONFIRMADA);

        assertThat(resultado.getEstado()).isEqualTo(AppointmentStatus.CONFIRMADA);
    }

    @Test
    @DisplayName("Transición inválida: CONFIRMADA -> CERRADA se rechaza")
    void transicionInvalida() {
        when(repository.findById(1L)).thenReturn(Optional.of(atencionEn(AppointmentStatus.CONFIRMADA)));

        assertThatThrownBy(() -> service.cambiarEstado(1L, AppointmentStatus.CERRADA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFIRMADA -> CERRADA");

        verify(repository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Una atención cerrada ya no puede cambiar de estado")
    void estadoFinalNoCambia() {
        when(repository.findById(1L)).thenReturn(Optional.of(atencionEn(AppointmentStatus.CERRADA)));

        assertThatThrownBy(() -> service.cambiarEstado(1L, AppointmentStatus.CANCELADA))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Atención inexistente: NotFoundException (404)")
    void atencionInexistente() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Filtro con 'from' posterior a 'to' se rechaza")
    void rangoDeFechasInvalido() {
        LocalDateTime desde = LocalDateTime.of(2026, 10, 2, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 10, 1, 0, 0);

        assertThatThrownBy(() -> service.filtrar(null, desde, hasta, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
