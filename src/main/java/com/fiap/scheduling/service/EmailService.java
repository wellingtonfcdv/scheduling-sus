package com.fiap.scheduling.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@scheduling-system.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendAppointmentConfirmation(String patientEmail, String patientName, String dateTime) {
        String subject = "Confirmação de Agendamento de Retorno";
        String body = String.format("Olá %s,\n\nSeu agendamento de retorno foi realizado com sucesso para o dia e horário: %s.\n\nAtenciosamente,\nEquipe Médica", 
                patientName, dateTime);
        sendEmail(patientEmail, subject, body);
    }
}
