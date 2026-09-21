package cl.duoc.vidasalud.ms_vidasalud_appointments.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "APPOINTMENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "appointment_seq"
    )
    @SequenceGenerator(
        name = "appointment_seq",
        sequenceName = "APPOINTMENT_SEQ",
        allocationSize = 1
    )
    private Long id;

    @NotNull
    @Column(name = "PACIENTE_ID", nullable = false)
    private Long pacienteId;

    @NotNull
    @Column(name = "PRESTACION_ID", nullable = false)
    private Long prestacionId;

    @Column(name = "BOX_ID")
    private Long boxId;

    @NotNull
    @Column(name = "FECHA_HORA", nullable = false)
    private LocalDateTime fechaHora;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false)
    private AppointmentStatus estado;
}