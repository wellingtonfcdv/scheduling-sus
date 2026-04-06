package com.fiap.scheduling.service;

import com.fiap.scheduling.entity.Appointment;
import com.fiap.scheduling.entity.Patient;
import com.fiap.scheduling.entity.Professional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.admin-email:admin@unidade-sus.com}")
    private String adminEmail;

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

    /**
     * ✅ NOVO: Email para ADMIN notificando novo agendamento pendente de confirmação
     */
    @Async
    public void sendPendingAppointmentNotificationToAdmin(Appointment appointment) {
        String subject = "⏳ [PENDENTE] Novo agendamento aguardando confirmação - " + 
                         appointment.getAppointmentDateTime().toLocalDate();
        
        String body = String.format(
            "Novo agendamento foi criado automaticamente e aguarda sua confirmação:\n\n" +
            "═══════════════════════════════════════\n" +
            "PACIENTE: %s\n" +
            "PROFISSIONAL: %s\n" +
            "DATA: %s\n" +
            "HORÁRIO: %s\n" +
            "PRIORIDADE: %s\n" +
            "═══════════════════════════════════════\n\n" +
            "⚠️  AÇÃO NECESSÁRIA:\n" +
            "Confirme o agendamento no sistema para notificar o paciente.\n" +
            "Se precisa rejeitar, será necessário ligar para o paciente.\n\n" +
            "Status: PENDENTE_CONFIRMACAO",
            appointment.getPatient().getName(),
            appointment.getProfessional().getName(),
            appointment.getAppointmentDateTime().toLocalDate(),
            appointment.getAppointmentDateTime().toLocalTime(),
            appointment.getReturnRequest().getPriority()
        );
        
        sendEmail(adminEmail, subject, body);
    }

    /**
     * ✅ NOVO: Email de aviso quando não há slots disponíveis para alocar paciente
     */
    @Async
    public void sendWarningNoSlotAvailable(Professional professional, Patient patient) {
        String subject = "⚠️  [ATENÇÃO] Sem slots disponíveis - Ação manual necessária";
        
        String body = String.format(
            "O sistema tentou alocar automaticamente um agendamento mas não encontrou slots disponíveis.\n\n" +
            "PACIENTE: %s\n" +
            "PROFISSIONAL: %s\n\n" +
            "⚠️  AÇÃO NECESSÁRIA:\n" +
            "1. Verifique a disponibilidade da agenda do profissional\n" +
            "2. Contate o paciente para reagendar manualmente\n" +
            "3. Relacione como 'Sem vagas disponíveis' no sistema\n",
            patient.getName(),
            professional.getName()
        );
        
        sendEmail(adminEmail, subject, body);
    }

    /**
     * ✅ NOVO: Email para PACIENTE confirmando agendamento após aprovação do admin
     */
    @Async
    public void sendConfirmedAppointmentNotificationToPatient(Appointment appointment) {
        String subject = "✅ Sua consulta está confirmada!";
        
        String body = String.format(
            "Olá %s,\n\n" +
            "Sua consulta foi confirmada! 🎉\n\n" +
            "DATA: %s\n" +
            "HORÁRIO: %s\n" +
            "PROFISSIONAL: %s\n\n" +
            "✓ Compareça 10 minutos antes do horário\n" +
            "✓ Se não puder comparecer, ligue na unidade com antecedência\n\n" +
            "Esperamos por você!",
            appointment.getPatient().getName(),
            appointment.getAppointmentDateTime().toLocalDate(),
            appointment.getAppointmentDateTime().toLocalTime(),
            appointment.getProfessional().getName()
        );
        
        sendEmail(appointment.getPatient().getEmail(), subject, body);
    }

    /**
     * ✅ NOVO: Email para PACIENTE informando rejeição e que será contatado
     */
    @Async
    public void sendRejectionNotificationToPatient(Appointment appointment, String razao) {
        String subject = "ℹ️ Alteração no seu agendamento";
        
        String body = String.format(
            "Olá %s,\n\n" +
            "Sua consulta foi reagendada.\n\n" +
            "Motivo: %s\n\n" +
            "A unidade entrará em contato pelo telefone %s para agendar novo horário.\n\n" +
            "Obrigado pela compreensão.",
            appointment.getPatient().getName(),
            razao != null ? razao : "Indisponibilidade",
            appointment.getPatient().getPhone() != null ? appointment.getPatient().getPhone() : "[cadastre seu telefone]"
        );
        
        sendEmail(appointment.getPatient().getEmail(), subject, body);
    }

    /**
     * ✅ NOVO: Email para PACIENTE informando cancelamento de consulta confirmada
     */
    @Async
    public void sendCancellationNotificationToPatient(Appointment appointment, String motivo) {
        String subject = "❌ Sua consulta foi cancelada";
        
        String body = String.format(
            "Olá %s,\n\n" +
            "Infelizmente sua consulta marcada para %s às %s foi cancelada.\n\n" +
            "Motivo: %s\n\n" +
            "A unidade entrará em contato pelo telefone %s para agendar um novo horário.\n\n" +
            "Agradecemos sua compreensão!",
            appointment.getPatient().getName(),
            appointment.getAppointmentDateTime().toLocalDate(),
            appointment.getAppointmentDateTime().toLocalTime(),
            motivo != null ? motivo : "Indisponibilidade",
            appointment.getPatient().getPhone() != null ? appointment.getPatient().getPhone() : "[cadastre seu telefone]"
        );
        
        sendEmail(appointment.getPatient().getEmail(), subject, body);
    }
}
