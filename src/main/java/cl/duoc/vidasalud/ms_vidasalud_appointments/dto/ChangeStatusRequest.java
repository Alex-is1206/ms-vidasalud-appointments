package cl.duoc.vidasalud.ms_vidasalud_appointments.dto;

import cl.duoc.vidasalud.ms_vidasalud_appointments.model.AppointmentStatus;

public record ChangeStatusRequest(
        AppointmentStatus status
) {
}