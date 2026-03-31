package com.fiap.scheduling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@scheduling-system.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("E-mail enviado com sucesso para: {}", to);
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail para {}: {}", to, e.getMessage());
            // Não relançamos a exceção para não quebrar a transação de agendamento
        }
    }

    public void sendAppointmentConfirmation(String patientEmail, String patientName, String dateTime) {
        String subject = "Confirmação de Agendamento de Retorno";
        String body = String.format("Olá %s,\n\nSeu agendamento de retorno foi realizado com sucesso para o dia e horário: %s.\n\nAtenciosamente,\nEquipe Médica", 
                patientName, dateTime);
        sendEmail(patientEmail, subject, body);
    }
}
