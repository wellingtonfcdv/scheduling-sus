package com.fiap.scheduling.controller;

import com.fiap.scheduling.dto.ReturnRequestDTO;
import com.fiap.scheduling.entity.Patient;
import com.fiap.scheduling.entity.Professional;
import com.fiap.scheduling.entity.ReturnRequest;
import com.fiap.scheduling.service.ReturnRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/return-requests")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @PostMapping
    public ResponseEntity<ReturnRequestDTO> create(@RequestBody ReturnRequestDTO dto) {
        // 1. Converte DTO para Entity para processamento no Service
        ReturnRequest request = ReturnRequest.builder()
                .professional(Professional.builder().id(dto.getProfessionalId()).build())
                .patient(Patient.builder().id(dto.getPatientId()).build())
                .priority(dto.getPriority())
                .deadline(dto.getDeadline())
                .description(dto.getDescription())
                .notes(dto.getNotes())
                .build();

        // 2. Chama o service que salva e valida
        ReturnRequest savedEntity = returnRequestService.createRequest(request);

        // 3. Converte a Entity salva de volta para DTO (usando o método de conversão que criamos no Service)
        // Isso garante que o JSON de resposta não tenha os proxies do Hibernate
        return ResponseEntity.ok(returnRequestService.getById(savedEntity.getId()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ReturnRequestDTO>> getPending() {
        // Agora o Service já retorna List<ReturnRequestDTO>, combinando perfeitamente
        return ResponseEntity.ok(returnRequestService.getPendingRequestsOrdered());
    }
}
