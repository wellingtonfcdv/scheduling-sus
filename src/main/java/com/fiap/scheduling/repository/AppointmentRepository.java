package com.fiap.scheduling.repository;

import com.fiap.scheduling.entity.Appointment;
import com.fiap.scheduling.entity.Professional;
import com.fiap.scheduling.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByProfessionalAndAppointmentDateTimeBetween(Professional professional, LocalDateTime start, LocalDateTime end);
    boolean existsByProfessionalAndAppointmentDateTime(Professional professional, LocalDateTime dateTime);
    boolean existsByReturnRequest(ReturnRequest returnRequest);
}
