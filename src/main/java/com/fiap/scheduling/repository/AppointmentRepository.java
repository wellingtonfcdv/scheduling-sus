package com.fiap.scheduling.repository;

import com.fiap.scheduling.entity.Appointment;
import com.fiap.scheduling.entity.Professional;
import com.fiap.scheduling.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    
    // Busca agendamentos entre datas
    List<Appointment> findByProfessionalAndAppointmentDateTimeBetween(Professional professional, LocalDateTime start, LocalDateTime end);
    
    // Verifica conflito - considera TODO appointment (inclusive cancelado)
    boolean existsByProfessionalAndAppointmentDateTime(Professional professional, LocalDateTime dateTime);
    
    // Verifica conflito - APENAS agendamentos CONFIRMADOS ou PENDENTES (ignora CANCELADO)
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM Appointment a " +
           "WHERE a.professional = :professional " +
           "AND a.appointmentDateTime = :dateTime " +
           "AND a.status IN ('CONFIRMADO', 'PENDENTE_CONFIRMACAO')")
    boolean existsConflictByProfessionalDateTime(@Param("professional") Professional professional, 
                                                  @Param("dateTime") LocalDateTime dateTime);
    
    // Lista agendamentos confirmados do profissional entre datas
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.professional = :professional " +
           "AND a.appointmentDateTime BETWEEN :startDate AND :endDate " +
           "AND a.status IN ('CONFIRMADO', 'PENDENTE_CONFIRMACAO') " +
           "ORDER BY a.appointmentDateTime ASC")
    List<Appointment> findAgendaByProfessionalAndDateRange(
            @Param("professional") Professional professional,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // Verifica se ReturnRequest tem agendamento
    boolean existsByReturnRequest(ReturnRequest returnRequest);

    // ✅ NOVO: Busca agendamentos pendentes de um profissional em uma data específica
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.professional.id = :professionalId " +
           "AND CAST(a.appointmentDateTime AS date) = :data " +
           "AND a.status = :status " +
           "ORDER BY a.appointmentDateTime ASC")
    List<Appointment> findByProfessionalIdAndAppointmentDateAndStatus(
            @Param("professionalId") UUID professionalId,
            @Param("data") LocalDate data,
            @Param("status") String status);

    // ✅ NOVO: Busca TODOS os agendamentos pendentes de um profissional
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.professional.id = :professionalId " +
           "AND a.status = :status " +
           "ORDER BY a.appointmentDateTime ASC")
    List<Appointment> findByProfessionalIdAndStatus(
            @Param("professionalId") UUID professionalId,
            @Param("status") String status);

    // ✅ NOVO: Busca a AGENDA COMPLETA do profissional (CONFIRMADO + PENDENTE_CONFIRMACAO)
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.professional.id = :professionalId " +
           "AND a.status IN ('CONFIRMADO', 'PENDENTE_CONFIRMACAO') " +
           "ORDER BY a.appointmentDateTime ASC")
    List<Appointment> findByProfessionalAndStatusInOrderByAppointmentDateTime(
            @Param("professionalId") UUID professionalId);
}
