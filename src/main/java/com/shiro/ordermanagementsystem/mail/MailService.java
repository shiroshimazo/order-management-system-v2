package com.shiro.ordermanagementsystem.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

public final class MailService {

    private MailService() {}

    public static void sendVerificationCode(String toEmail, String code)
            throws MessagingException, UnsupportedEncodingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", MailConfig.SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(MailConfig.SMTP_PORT));
        props.put("mail.smtp.ssl.trust", MailConfig.SMTP_HOST);

        String appPassword = MailConfig.APP_PASSWORD.replace(" ", "");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MailConfig.USERNAME, appPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(MailConfig.USERNAME, MailConfig.FROM_NAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Your Password Reset Code");
        message.setContent(buildHtmlBody(code), "text/html; charset=utf-8");

        Transport.send(message);
    }

    private static String buildHtmlBody(String code) {
        return """
               <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;
                           padding: 32px; background: #fdfdfd; border: 1px solid #e8e8e8;
                           border-radius: 12px;">
                 <h2 style="color: #000; margin: 0 0 8px 0;">Password Reset</h2>
                 <p style="color: #666; font-size: 14px; margin: 0 0 24px 0;">
                   Use the code below to reset your password. It expires in 3 minutes.
                 </p>
                 <div style="background: #f7f7f7; border: 1px solid #e0e0e0; border-radius: 10px;
                             padding: 20px; text-align: center; font-size: 28px;
                             letter-spacing: 8px; font-weight: bold; color: #D92A1C;">
                   %s
                 </div>
                 <p style="color: #999; font-size: 12px; margin: 24px 0 0 0;">
                   If you did not request this, you can safely ignore this email.
                 </p>
               </div>
               """.formatted(code);
    }
}
