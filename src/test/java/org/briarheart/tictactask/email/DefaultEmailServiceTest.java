package org.briarheart.tictactask.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DefaultEmailServiceTest {
    private DefaultEmailService service;
    private MailProperties mailProperties;
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setUsername("no-reply@application.com");
        mailSender = mock(JavaMailSender.class);
        service = new DefaultEmailService(mailProperties, mailSender);
    }

    @Test
    void shouldThrowExceptionOnConstructWhenMailPropertiesAreNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultEmailService(null, mailSender));
        assertEquals("Mail properties must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnConstructWhenMailSenderIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultEmailService(new MailProperties(), null));
        assertEquals("Mail sender must not be null", e.getMessage());
    }

    @Test
    void shouldSendEmail() {
        String to = "test@mail.com";
        String subject = "Hello";
        String text = "world!";
        service.sendEmail(to, subject, text);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSetFromAddressFromMailPropertiesOnEmailSend() {
        String to = "test@mail.com";
        String subject = "Hello";
        String text = "world!";
        service.sendEmail(to, subject, text);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals(mailProperties.getUsername(), message.getFrom());
    }

    @Test
    void shouldThrowExceptionOnEmailSendWhenEmailAddressIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.sendEmail(null, "Hello", "world!"));
        assertEquals("Email address must not be null or empty", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnEmailSendWhenEmailAddressIsEmpty() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.sendEmail("", "Hello", "world!"));
        assertEquals("Email address must not be null or empty", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnEmailSendWhenEmailSubjectIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.sendEmail("test@mail.com", null, "world!"));
        assertEquals("Email subject must not be null or empty", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnEmailSendWhenEmailSubjectIsEmpty() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.sendEmail("test@mail.com", "", "world!"));
        assertEquals("Email subject must not be null or empty", e.getMessage());
    }
}