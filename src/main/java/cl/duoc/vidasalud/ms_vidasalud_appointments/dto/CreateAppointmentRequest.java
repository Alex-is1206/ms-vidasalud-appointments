package cl.duoc.vidasalud.ms_vidasalud_appointments.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAppointmentRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        String pacienteEmail,

        @NotNull
        Long prestacionId,

        Long boxId,

        @NotNull
        LocalDateTime fechaHora
) {
}
