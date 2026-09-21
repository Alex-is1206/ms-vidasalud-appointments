package cl.duoc.vidasalud.ms_vidasalud_appointments.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.ms_vidasalud_appointments.dto.ChangeStatusRequest;
import cl.duoc.vidasalud.ms_vidasalud_appointments.dto.CreateAppointmentRequest;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.Appointment;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.AppointmentStatus;
import cl.duoc.vidasalud.ms_vidasalud_appointments.service.AppointmentService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Appointment> crear(
            @Valid @RequestBody CreateAppointmentRequest request) {

        Appointment appointment = new Appointment();
        appointment.setPacienteEmail(request.pacienteEmail());
        appointment.setPrestacionId(request.prestacionId());
        appointment.setBoxId(request.boxId());
        appointment.setFechaHora(request.fechaHora());

        Appointment creada = service.crear(appointment);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(creada);
    }

    @GetMapping
    public List<Appointment> listar(
            @RequestParam(required = false)
            AppointmentStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(required = false)
            String pacienteEmail) {

        return service.filtrar(status, from, to, pacienteEmail);
    }

    @GetMapping("/{id}")
    public Appointment buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}/status")
    public Appointment cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {

        return service.cambiarEstado(id, request.status());
    }
}
