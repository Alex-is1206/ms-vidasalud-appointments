package cl.duoc.vidasalud.ms_vidasalud_appointments.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.duoc.vidasalud.ms_vidasalud_appointments.model.Appointment;
import cl.duoc.vidasalud.ms_vidasalud_appointments.model.AppointmentStatus;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    @Query("""
        SELECT a
        FROM Appointment a
        WHERE (:status IS NULL OR a.estado = :status)
          AND (:from IS NULL OR a.fechaHora >= :from)
          AND (:to IS NULL OR a.fechaHora <= :to)
        ORDER BY a.fechaHora ASC
        """)
    List<Appointment> filtrar(
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}