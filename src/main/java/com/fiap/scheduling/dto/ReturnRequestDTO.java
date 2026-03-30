package com.fiap.scheduling.dto;

import com.fiap.scheduling.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnRequestDTO {
    private UUID id;
    private UUID professionalId;
    private String professionalName;
    private UUID patientId;
    private String patientName;
    private Priority priority;
    private LocalDate deadline;
    private String notes;
    private RequestStatus status;
}
