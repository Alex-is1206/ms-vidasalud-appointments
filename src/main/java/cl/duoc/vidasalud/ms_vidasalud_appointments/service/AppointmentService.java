package cl.duoc.vidasalud.ms_vidasalud_appointments.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.ms_vidasalud_appointments.exception.NotFoundException;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.Appointment;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.AppointmentStatus;
import cl.duoc.vidasalud.ms_vidasalud_appointments.repository.AppointmentRepository;

@Service
public class AppointmentService {

    private final AppointmentRepository repository;

    public AppointmentService(AppointmentRepository repository) {
        this.repository = repository;
    }

    // Crear atención
    public Appointment crear(Appointment appointment) {

        // Toda atención nueva comienza como SOLICITADA
        appointment.setId(null);
        appointment.setEstado(AppointmentStatus.SOLICITADA);

        return repository.save(appointment);
    }

    // Listar todas
    public List<Appointment> listar() {
        return repository.findAll();
    }

    // Buscar por ID
    public Appointment buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                    new NotFoundException("Atención no encontrada con ID: " + id)
                );
    }

    // Cambiar estado
    public Appointment cambiarEstado(Long id, AppointmentStatus nuevoEstado) {

        Appointment appointment = buscarPorId(id);

        validarTransicion(appointment.getEstado(), nuevoEstado);

        appointment.setEstado(nuevoEstado);

        return repository.save(appointment);
    }

    // Filtrar atenciones
    public List<Appointment> filtrar(
            AppointmentStatus status,
            LocalDateTime from,
            LocalDateTime to,
            String pacienteEmail) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "La fecha 'from' no puede ser posterior a 'to'"
            );
        }

        return repository.filtrar(status, from, to, pacienteEmail);
    }

    private void validarTransicion(
            AppointmentStatus actual,
            AppointmentStatus nuevo) {

        boolean valida = switch (actual) {

            case SOLICITADA ->
                nuevo == AppointmentStatus.CONFIRMADA ||
                nuevo == AppointmentStatus.CANCELADA;

            case CONFIRMADA ->
                nuevo == AppointmentStatus.EN_ESPERA ||
                nuevo == AppointmentStatus.CANCELADA;

            case EN_ESPERA ->
                nuevo == AppointmentStatus.EN_ATENCION ||
                nuevo == AppointmentStatus.CANCELADA;

            case EN_ATENCION ->
                nuevo == AppointmentStatus.CERRADA;

            case CERRADA, CANCELADA ->
                false;
        };

        if (!valida) {
            throw new IllegalStateException(
                "Transición de estado no permitida: "
                + actual + " -> " + nuevo
            );
        }
    }
}