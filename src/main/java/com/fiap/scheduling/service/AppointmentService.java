package com.fiap.scheduling.service;

import com.fiap.scheduling.dto.AppointmentDTO;
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

    private static final LocalTime HORARIO_INICIO = LocalTime.of(8, 0);
    private static final LocalTime HORARIO_FIM = LocalTime.of(17, 0);
    private static final int MINUTOS_SLOT = 30;

    /**
     * Retorna os próximos N dias que possuem pelo menos 1 slot disponível
     * @param professional Profissional
     * @param diasAhead Quantos dias no futuro verificar
     * @return List<LocalDate> com datas que têm vagas
     */
    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(Professional professional, int diasAhead) {
        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate startDate = LocalDate.now();

        for (int i = 0; i < diasAhead; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            
            // Pula finais de semana (opcional - remova se quiser incluir)
            // Comentado para permitir agendamento nos fins de semana
            // if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || 
            //     currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            
            List<LocalDateTime> slotsForDay = getAvailableSlots(professional, currentDate);
            if (!slotsForDay.isEmpty()) {
                availableDates.add(currentDate);
            }
        }
        
        return availableDates;
    }

    /**
     * Retorna os horários disponíveis para uma data específica
     * Este é fundamentalmente o mesmo que getAvailableSlots, mas com melhor semântica
     */
    @Transactional(readOnly = true)
    public List<LocalDateTime> getAvailableSlotsForDate(Professional professional, LocalDate date) {
        return getAvailableSlots(professional, date);
    }

    /**
     * Encontra o próximo slot disponível respeitando a PRIORIDADE do retorno
     * 
     * Regras de Prioridade:
     * ├─ URGENTE  → Próximos 3 dias
     * ├─ ALTA     → Próximos 7 dias
     * ├─ MEDIA    → Próximos 30 dias
     * └─ BAIXA    → Próximos 90 dias
     */
    @Transactional(readOnly = true)
    public LocalDateTime findNextAvailableSlotByPriority(Professional professional, String priority) {
        LocalDate currentDate = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        
        // Define a janela PREFERENCIAL baseado na prioridade
        LocalDate startDate = getStartDateByPriority(priority);
        LocalDate preferredEndDate = getEndDateByPriority(priority);
        
        // Primeiro: procura na janela preferencial
        for (LocalDate date = startDate; !date.isAfter(preferredEndDate); date = date.plusDays(1)) {
            List<LocalDateTime> slotsForDay = getAvailableSlots(professional, date);
            
            // Filtrar slots que já passaram (apenas para hoje)
            if (date.isEqual(LocalDate.now())) {
                slotsForDay = slotsForDay.stream()
                    .filter(slot -> slot.isAfter(now))
                    .toList();
            }
            
            if (!slotsForDay.isEmpty()) {
                return slotsForDay.get(0); // Retorna primeiro slot disponível
            }
        }
        
        // Se não encontrou na janela preferencial, continua procurando nos próximos 180 dias
        LocalDate extendedEndDate = LocalDate.now().plusDays(180);
        for (LocalDate date = preferredEndDate.plusDays(1); !date.isAfter(extendedEndDate); date = date.plusDays(1)) {
            List<LocalDateTime> slotsForDay = getAvailableSlots(professional, date);
            
            if (!slotsForDay.isEmpty()) {
                return slotsForDay.get(0); // Retorna primeiro slot disponível
            }
        }

        // Se ainda não encontrou, erro
        throw new RuntimeException(
            "Nenhum slot disponível para prioridade " + priority + 
            " nos próximos 180 dias. Tente novamente mais tarde."
        );
    }
    
    /**
     * Retorna a data inicial para buscar slot baseado na prioridade
     */
    private LocalDate getStartDateByPriority(String priority) {
        LocalDate now = LocalDate.now();
        if ("URGENTE".equalsIgnoreCase(priority)) {
            return now;  // Começa hoje (dia 0)
        } else if ("ALTA".equalsIgnoreCase(priority)) {
            return now.plusDays(4);  // Começa no dia 4 (APÓS URGENTE)
        } else if ("MEDIA".equalsIgnoreCase(priority)) {
            return now.plusDays(11);  // Começa no dia 11 (APÓS ALTA)
        } else {
            return now.plusDays(41); // Começa no dia 41 (APÓS MEDIA)
        }
    }
    
    /**
     * Retorna a data final para buscar slot baseado na prioridade
     */
    private LocalDate getEndDateByPriority(String priority) {
        LocalDate now = LocalDate.now();
        if ("URGENTE".equalsIgnoreCase(priority)) {
            return now.plusDays(2);  // Dias 0-2 (3 dias total)
        } else if ("ALTA".equalsIgnoreCase(priority)) {
            return now.plusDays(10);  // Dias 4-10 (7 dias total)
        } else if ("MEDIA".equalsIgnoreCase(priority)) {
            return now.plusDays(40);  // Dias 11-40 (30 dias total)
        } else {
            return now.plusDays(130); // Dias 41-130 (90 dias total)
        }
    }

    /**
     * Retorna o número máximo de dias para buscar slot baseado na prioridade
     */
    private int getDiasMaximoByPriority(String priority) {
        if ("URGENTE".equalsIgnoreCase(priority)) {
            return 3;   // URGENTE: próximos 3 dias
        } else if ("ALTA".equalsIgnoreCase(priority)) {
            return 7;   // ALTA: próximos 7 dias
        } else if ("MEDIA".equalsIgnoreCase(priority)) {
            return 30;  // MEDIA: próximos 30 dias
        } else {
            return 90;  // BAIXA: próximos 90 dias
        }
    }

    public List<LocalDateTime> getAvailableSlots(Professional professional, LocalDate date) {
        List<LocalDateTime> slots = new ArrayList<>();

        LocalDateTime current = LocalDateTime.of(date, HORARIO_INICIO);
        LocalDateTime end = LocalDateTime.of(date, HORARIO_FIM);

        while (current.isBefore(end)) {
            // ✅ Valida apenas agendamentos CONFIRMADOS/PENDENTES (ignora CANCELADOS)
            if (!appointmentRepository.existsConflictByProfessionalDateTime(professional, current)) {
                slots.add(current);
            }
            current = current.plusMinutes(MINUTOS_SLOT);
        }
        return slots;
    }

    @Transactional
    public AppointmentDTO requestAppointment(UUID requestId, LocalDateTime dateTime) {
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

        // ✅ Valida APENAS agendamentos CONFIRMADOS/PENDENTES (melhoria)
        if (appointmentRepository.existsConflictByProfessionalDateTime(request.getProfessional(), dateTime)) {
            throw new RuntimeException("Horário não disponível (conflito com outro agendamento)");
        }

        Appointment appointment = Appointment.builder()
                .returnRequest(request)
                .professional(request.getProfessional())
                .patient(request.getPatient())
                .appointmentDateTime(dateTime)
                .status("PENDENTE_CONFIRMACAO")
                .confirmationLink(UUID.randomUUID().toString())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return convertToDTO(savedAppointment);
    }

    /**
     * Retorna a agenda do profissional para um dia específico
     */
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAgendaProfissional(UUID professionalId, LocalDate date) {
        Professional professional = Professional.builder().id(professionalId).build();
        
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.of(0, 0));
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.of(23, 59));
        
        List<Appointment> agendamentos = appointmentRepository.findAgendaByProfessionalAndDateRange(
                professional, startOfDay, endOfDay);
        
        return agendamentos.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * ✅ NOVO: Retorna a AGENDA COMPLETA do profissional (sem filtro de data)
     * Mostra todos os agendamentos CONFIRMADOS + PENDENTES, ordenado por data
     */
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAgendaProfissionalCompleta(UUID professionalId) {
        // Busca todos os agendamentos CONFIRMADOS e PENDENTES_CONFIRMACAO (ignora REJEITADOS)
        List<Appointment> agendamentos = appointmentRepository.findByProfessionalAndStatusInOrderByAppointmentDateTime(
                professionalId);
        
        return agendamentos.stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public AppointmentDTO confirmAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        appointment.setStatus("CONFIRMADO");
        
        ReturnRequest request = appointment.getReturnRequest();
        request.setStatus(ReturnRequestStatus.AGENDADO);
        returnRequestRepository.save(request);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return convertToDTO(savedAppointment);
    }

    /**
     * Rejeita um agendamento pendente de confirmação
     * Volta ReturnRequest para PENDENTE para possível reagendamento
     */
    @Transactional
    public AppointmentDTO rejectAppointment(UUID appointmentId, String razao) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        if (!appointment.getStatus().equals("PENDENTE_CONFIRMACAO")) {
            throw new RuntimeException(
                "Only PENDENTE_CONFIRMACAO appointments can be rejected. Current status: " + appointment.getStatus()
            );
        }

        appointment.setStatus("REJEITADO");
        
        ReturnRequest request = appointment.getReturnRequest();
        request.setStatus(ReturnRequestStatus.PENDENTE); // Volta a PENDENTE para possível reagendamento
        returnRequestRepository.save(request);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // ✅ NOVO: Dispara email de rejeição para o paciente
        emailService.sendRejectionNotificationToPatient(savedAppointment, razao);

        return convertToDTO(savedAppointment);
    }

    /**
     * ✅ NOVO: Cancela um agendamento CONFIRMADO
     * Diferente de REJEITAR: rejeição é antes da confirmação, cancelamento é após confirmação
     * Volta ReturnRequest para PENDENTE para possível reagendamento
     * Slot fica livre para outro paciente
     */
    @Transactional
    public AppointmentDTO cancelAppointment(UUID appointmentId, String motivo) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        if (!appointment.getStatus().equals("CONFIRMADO")) {
            throw new RuntimeException(
                "Only CONFIRMADO appointments can be cancelled. Current status: " + appointment.getStatus()
            );
        }

        appointment.setStatus("CANCELADO");
        
        ReturnRequest request = appointment.getReturnRequest();
        request.setStatus(ReturnRequestStatus.PENDENTE); // Volta a PENDENTE para possível reagendamento
        returnRequestRepository.save(request);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // ✅ Dispara email de cancelamento para o paciente
        emailService.sendCancellationNotificationToPatient(savedAppointment, motivo);

        return convertToDTO(savedAppointment);
    }

    /**
     * ✅ NOVO: Confirma TODAS as consultas pendentes de um profissional em uma data específica
     */
    @Transactional
    public java.util.Map<String, Object> confirmAppointmentsBatch(UUID professionalId, LocalDate date) {
        List<Appointment> pendingAppointments = appointmentRepository.findByProfessionalIdAndAppointmentDateAndStatus(
            professionalId, 
            date, 
            "PENDENTE_CONFIRMACAO"
        );

        int totalConfirmed = 0;
        for (Appointment appointment : pendingAppointments) {
            appointment.setStatus("CONFIRMADO");
            ReturnRequest request = appointment.getReturnRequest();
            request.setStatus(ReturnRequestStatus.AGENDADO);
            returnRequestRepository.save(request);
            appointmentRepository.save(appointment);
            totalConfirmed++;
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("totalConfirmados", totalConfirmed);
        response.put("data", date);
        response.put("professionalId", professionalId);
        response.put("mensagem", String.format("✅ %d agendamentos confirmados para %s", totalConfirmed, date));
        
        return response;
    }

    /**
     * ✅ NOVO: Confirma TODAS as consultas pendentes de um profissional (sem filtro de data)
     */
    @Transactional
    public java.util.Map<String, Object> confirmAllPendingAppointmentsByProfessional(UUID professionalId) {
        List<Appointment> pendingAppointments = appointmentRepository.findByProfessionalIdAndStatus(
            professionalId,
            "PENDENTE_CONFIRMACAO"
        );

        int totalConfirmed = 0;
        for (Appointment appointment : pendingAppointments) {
            appointment.setStatus("CONFIRMADO");
            ReturnRequest request = appointment.getReturnRequest();
            request.setStatus(ReturnRequestStatus.AGENDADO);
            returnRequestRepository.save(request);
            appointmentRepository.save(appointment);
            totalConfirmed++;
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("totalConfirmados", totalConfirmed);
        response.put("professionalId", professionalId);
        response.put("mensagem", String.format("✅ %d agendamentos pendentes confirmados para este profissional", totalConfirmed));
        
        return response;
    }

    private AppointmentDTO convertToDTO(Appointment entity) {
        return AppointmentDTO.builder()
                .id(entity.getId())
                .returnRequestId(entity.getReturnRequest().getId())
                .professionalId(entity.getProfessional().getId())
                .professionalName(entity.getProfessional().getName())
                .patientId(entity.getPatient().getId())
                .patientName(entity.getPatient().getName())
                .appointmentDateTime(entity.getAppointmentDateTime())
                .status(entity.getStatus())
                .confirmationLink(entity.getConfirmationLink())
                .build();
    }
}
