package com.fiap.scheduling.service;

import com.fiap.scheduling.entity.Appointment;
import com.fiap.scheduling.entity.Professional;
import com.fiap.scheduling.entity.ReturnRequest;
import com.fiap.scheduling.enums.ReturnRequestStatus;
import com.fiap.scheduling.repository.AppointmentRepository;
import com.fiap.scheduling.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final EmailService emailService;

    public List<LocalDateTime> getAvailableSlots(Professional professional, LocalDate date) {
        List<LocalDateTime> slots = new ArrayList<>();
        // Exemplo: Horário comercial das 08:00 às 17:00, slots de 30 min
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        LocalDateTime current = LocalDateTime.of(date, startTime);
        LocalDateTime end = LocalDateTime.of(date, endTime);

        while (current.isBefore(end)) {
            if (!appointmentRepository.existsByProfessionalAndAppointmentDateTime(professional, current)) {
                slots.add(current);
            }
            current = current.plusMinutes(30);
        }
        return slots;
    }

    @Transactional
    public Appointment requestAppointment(UUID requestId, LocalDateTime dateTime) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

        // Verifica se já existe um agendamento para esta solicitação
        if (appointmentRepository.existsByReturnRequest(request)) {
            throw new RuntimeException("Esta solicitação já possui um agendamento");
        }

        // Verifica se a solicitação já foi agendada/concluída
        if (request.getStatus() != ReturnRequestStatus.PENDENTE) {
            throw new RuntimeException("Solicitação não está disponível para agendamento (Status: " + request.getStatus() + ")");
        }

        if (appointmentRepository.existsByProfessionalAndAppointmentDateTime(request.getProfessional(), dateTime)) {
            throw new RuntimeException("Horário não disponível");
        }

        Appointment appointment = Appointment.builder()
                .returnRequest(request)
                .professional(request.getProfessional())
                .patient(request.getPatient())
                .appointmentDateTime(dateTime)
                .status("PENDENTE_CONFIRMACAO")
                .confirmationLink(UUID.randomUUID().toString())
                .build();

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment confirmAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        appointment.setStatus("CONFIRMADO");
        
        ReturnRequest request = appointment.getReturnRequest();
        request.setStatus(ReturnRequestStatus.AGENDADO);
        returnRequestRepository.save(request);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Disparo de e-mail após confirmação
        emailService.sendAppointmentConfirmation(
                savedAppointment.getPatient().getEmail(),
                savedAppointment.getPatient().getName(),
                savedAppointment.getAppointmentDateTime().toString()
        );

        return savedAppointment;
    }
}
