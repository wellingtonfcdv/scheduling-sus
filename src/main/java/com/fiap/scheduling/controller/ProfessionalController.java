package com.fiap.scheduling.controller;

import com.fiap.scheduling.entity.Professional;
import com.fiap.scheduling.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalRepository professionalRepository;

    @PostMapping
    public ResponseEntity<Professional> create(@RequestBody Professional professional) {
        return ResponseEntity.ok(professionalRepository.save(professional));
    }

    @GetMapping
    public ResponseEntity<List<Professional>> getAll() {
        return ResponseEntity.ok(professionalRepository.findAll());
    }
}
