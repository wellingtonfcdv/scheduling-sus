package com.fiap.scheduling.controller;

import com.fiap.scheduling.dto.AppointmentDTO;
import com.fiap.scheduling.entity.Appointment;
import com.fiap.scheduling.entity.Professional;
import com.fiap.scheduling.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/available-slots")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(
            @RequestParam UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        Professional professional = Professional.builder().id(professionalId).build();
        return ResponseEntity.ok(appointmentService.getAvailableSlots(professional, date));
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
}
