package cl.duoc.vidasalud.ms_vidasalud_appointments.dto;

import cl.duoc.vidasalud.ms_vidasalud_appointments.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(

        @NotNull
        AppointmentStatus status
) {
}
