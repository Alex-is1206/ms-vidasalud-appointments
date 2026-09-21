package cl.duoc.vidasalud.ms_vidasalud_appointments.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record CreateAppointmentRequest(

        @NotNull
        Long pacienteId,

        @NotNull
        Long prestacionId,

        Long boxId,

        @NotNull
        LocalDateTime fechaHora

) {
}