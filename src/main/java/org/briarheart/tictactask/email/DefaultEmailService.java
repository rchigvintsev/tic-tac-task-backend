package org.briarheart.tictactask.email;

import io.jsonwebtoken.lang.Assert;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class DefaultEmailService implements EmailService {
    private final MailProperties mailProperties;
    private final JavaMailSender mailSender;

    public DefaultEmailService(MailProperties mailProperties, JavaMailSender mailSender) {
        Assert.notNull(mailProperties, "Mail properties must not be null");
        Assert.notNull(mailSender, "Mail sender must not be null");

        this.mailProperties = mailProperties;
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String text) {
        Assert.hasLength(to, "Email address must not be null or empty");
        Assert.hasLength(subject, "Email subject must not be null or empty");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getUsername());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
