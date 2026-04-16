package com.fiap.scheduling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppointmentDTO {
    private UUID id;
    private UUID returnRequestId;
    private UUID professionalId;
    private String professionalName;
    private UUID patientId;
    private String patientName;
    private LocalDateTime appointmentDateTime;
    private String status;
    private String confirmationLink;
}
