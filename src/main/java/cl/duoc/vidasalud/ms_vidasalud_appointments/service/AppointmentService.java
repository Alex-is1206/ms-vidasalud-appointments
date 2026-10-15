package cl.duoc.vidasalud.ms_vidasalud_appointments.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.ms_vidasalud_appointments.exception.NotFoundException;
import cl.duoc.vidasalud.ms_vidasalud_appointments.mensajeria.PublicadorAtenciones;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.Appointment;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.AppointmentStatus;
import cl.duoc.vidasalud.ms_vidasalud_appointments.repository.AppointmentRepository;

@Service
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PublicadorAtenciones publicador;

    public AppointmentService(AppointmentRepository repository,
                              PublicadorAtenciones publicador) {
        this.repository = repository;
        this.publicador = publicador;
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

        Appointment guardada = repository.save(appointment);

        // El evento se publica solo despues de que el cambio quedo en la base de datos
        publicarSiCorresponde(guardada);

        return guardada;
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

    /**
     * Publica un evento en RabbitMQ cuando el nuevo estado es relevante para
     * otros servicios. Los estados intermedios no generan evento.
     */
    private void publicarSiCorresponde(Appointment atencion) {
        switch (atencion.getEstado()) {

            case CONFIRMADA -> publicador.atencionConfirmada(
                    atencion.getId(),
                    atencion.getPacienteEmail(),
                    atencion.getBoxId());

            case CANCELADA -> publicador.atencionCancelada(
                    atencion.getId(),
                    atencion.getPacienteEmail(),
                    "cancelada desde el cambio de estado");

            default -> { /* EN_ESPERA, EN_ATENCION y CERRADA no notifican */ }
        }
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