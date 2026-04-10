package com.fiap.scheduling.controller;

import com.fiap.scheduling.dto.AppointmentDTO;
import com.fiap.scheduling.entity.Professional;
import com.fiap.scheduling.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * GET /api/appointments/datas-disponiveis/{professionalId}?proximos=7
     * Retorna os próximos N dias que possuem slots disponíveis
     */
    @GetMapping("/datas-disponiveis/{professionalId}")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @PathVariable UUID professionalId,
            @RequestParam(defaultValue = "7") int proximos) {
        
        Professional professional = Professional.builder().id(professionalId).build();
        List<LocalDate> availableDates = appointmentService.getAvailableDates(professional, proximos);
        return ResponseEntity.ok(availableDates);
    }

    /**
     * GET /api/appointments/horarios-disponiveis/{professionalId}?data=YYYY-MM-DD
     * Retorna os horários disponíveis para uma data específica
     */
    @GetMapping("/horarios-disponiveis/{professionalId}")
    public ResponseEntity<List<LocalDateTime>> getAvailableTimeSlotsForDate(
            @PathVariable UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        
        Professional professional = Professional.builder().id(professionalId).build();
        List<LocalDateTime> slots = appointmentService.getAvailableSlotsForDate(professional, data);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(
            @RequestParam UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        Professional professional = Professional.builder().id(professionalId).build();
        return ResponseEntity.ok(appointmentService.getAvailableSlots(professional, date));
    }

    /**
     * GET /api/appointments/agenda-profissional/{professionalId}?data=YYYY-MM-DD
     * Retorna a agenda (agendamentos confirmados/pendentes) do profissional para um dia
     */
    @GetMapping("/agenda-profissional/{professionalId}")
    public ResponseEntity<List<AppointmentDTO>> getAgendaProfissional(
            @PathVariable UUID professionalId,
            @RequestParam(name = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        
        if (data != null) {
            // Com filtro de data
            List<AppointmentDTO> agenda = appointmentService.getAgendaProfissional(professionalId, data);
            return ResponseEntity.ok(agenda);
        } else {
            // Sem filtro - retorna agenda completa
            List<AppointmentDTO> agendaCompleta = appointmentService.getAgendaProfissionalCompleta(professionalId);
            return ResponseEntity.ok(agendaCompleta);
        }
    }

    @PostMapping("/request")
    public ResponseEntity<AppointmentDTO> requestAppointment(
            @RequestParam UUID requestId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        
        return ResponseEntity.ok(appointmentService.requestAppointment(requestId, dateTime));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<AppointmentDTO> confirmAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(id));
    }

    /**
     * POST /api/appointments/{id}/rejeitar
     * Rejeita um agendamento pendente de confirmação
     * Admin DEVE ligar para o paciente para reagendar
     */
    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<AppointmentDTO> rejectAppointment(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        
        String razao = body != null ? body.get("razao") : null;
        AppointmentDTO updated = appointmentService.rejectAppointment(id, razao);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/appointments/{id}/cancelar
     * Cancela um agendamento CONFIRMADO (diferente de rejeitar)
     * Admin DEVE ligar para o paciente se necessário
     * Motivo pode ser: desistência, impossibilidade, etc.
     */
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<AppointmentDTO> cancelAppointment(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        
        String motivo = body != null ? body.get("motivo") : null;
        AppointmentDTO updated = appointmentService.cancelAppointment(id, motivo);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/appointments/confirm-batch/por-data
     * Confirma TODAS as consultas pendentes de um profissional em uma data específica
     * 
     * Request:
     * {
     *   "professionalId": "uuid",
     *   "data": "2025-05-15"
     * }
     */
    @PostMapping("/confirm-batch/por-data")
    public ResponseEntity<Map<String, Object>> confirmAppointmentsByDate(
            @RequestBody Map<String, String> body) {
        
        UUID professionalId = UUID.fromString(body.get("professionalId"));
        LocalDate data = LocalDate.parse(body.get("data"));
        
        Map<String, Object> result = appointmentService.confirmAppointmentsBatch(professionalId, data);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/appointments/confirm-batch/por-medico
     * Confirma TODAS as consultas pendentes de um profissional (sem filtro de data)
     * 
     * Request:
     * {
     *   "professionalId": "uuid"
     * }
     */
    @PostMapping("/confirm-batch/por-medico")
    public ResponseEntity<Map<String, Object>> confirmAppointmentsByProfessional(
            @RequestBody Map<String, String> body) {
        
        UUID professionalId = UUID.fromString(body.get("professionalId"));
        Map<String, Object> result = appointmentService.confirmAllPendingAppointmentsByProfessional(professionalId);
        return ResponseEntity.ok(result);
    }
}
