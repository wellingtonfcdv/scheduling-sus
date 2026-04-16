package com.fiap.scheduling.service;

import com.fiap.scheduling.dto.ReturnRequestDTO;
import com.fiap.scheduling.entity.Appointment;
import com.fiap.scheduling.entity.Patient;
import com.fiap.scheduling.entity.Professional;
import com.fiap.scheduling.entity.ReturnRequest;
import com.fiap.scheduling.enums.ReturnRequestStatus;
import com.fiap.scheduling.repository.AppointmentRepository;
import com.fiap.scheduling.repository.PatientRepository;
import com.fiap.scheduling.repository.ProfessionalRepository;
import com.fiap.scheduling.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ProfessionalRepository professionalRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final EmailService emailService;

    @Transactional
    public ReturnRequest createRequest(ReturnRequest request) {
        Professional professional = professionalRepository.findById(request.getProfessional().getId())
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado com o ID: " + request.getProfessional().getId()));

        Patient patient = patientRepository.findById(request.getPatient().getId())
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado com o ID: " + request.getPatient().getId()));

        request.setProfessional(professional);
        request.setPatient(patient);
        request.setRequestDate(LocalDateTime.now());
        request.setStatus(ReturnRequestStatus.PENDENTE);

        ReturnRequest savedRequest = returnRequestRepository.save(request);

        // ✅ NOVO: Auto-alocar agendamento baseado na prioridade
        try {
            LocalDateTime appointmentDateTime = appointmentService.findNextAvailableSlotByPriority(
                professional, 
                request.getPriority().toString()
            );

            Appointment appointment = Appointment.builder()
                    .returnRequest(savedRequest)
                    .professional(professional)
                    .patient(patient)
                    .appointmentDateTime(appointmentDateTime)
                    .status("PENDENTE_CONFIRMACAO")
                    .confirmationLink(UUID.randomUUID().toString())
                    .build();

            appointmentRepository.save(appointment);

        } catch (RuntimeException e) {
            // Se não conseguir alocar, apenas registra log
            System.err.println("Aviso: Não foi possível alocar automaticamente o agendamento. " + e.getMessage());
        }

        return savedRequest;
    }

    // ALTERADO: Agora retorna uma lista de DTOs para evitar erro de Lazy Loading/Proxy
    public List<ReturnRequestDTO> getPendingRequestsOrdered() {
        List<ReturnRequest> entities = returnRequestRepository.findByStatusOrderByPriorityDescRequestDateAsc(ReturnRequestStatus.PENDENTE);

        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ReturnRequestDTO getById(UUID id) {
        ReturnRequest entity = returnRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));
        return convertToDTO(entity);
    }

    // MÉTODO NOVO: Realiza a conversão manual de Entity para DTO
    private ReturnRequestDTO convertToDTO(ReturnRequest entity) {
        ReturnRequestDTO dto = new ReturnRequestDTO();

        dto.setId(entity.getId());
        dto.setPriority(entity.getPriority());
        dto.setDeadline(entity.getDeadline());
        dto.setDescription(entity.getDescription());
        dto.setNotes(entity.getNotes());
        dto.setStatus(entity.getStatus());

        // Mapeia IDs e Nomes para o DTO (isso resolve o erro ByteBuddyInterceptor)
        if (entity.getProfessional() != null) {
            dto.setProfessionalId(entity.getProfessional().getId());
            dto.setProfessionalName(entity.getProfessional().getName());
        }

        if (entity.getPatient() != null) {
            dto.setPatientId(entity.getPatient().getId());
            dto.setPatientName(entity.getPatient().getName());
        }

        return dto;
    }
}
