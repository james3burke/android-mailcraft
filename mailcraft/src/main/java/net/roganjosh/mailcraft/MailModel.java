package net.roganjosh.mailcraft;

import java.util.List;

import javax.mail.internet.InternetAddress;

/**
 *
 */
public class MailModel {

    private InternetAddress sender;
    private List<InternetAddress> recipients;
    private String subject;
    private String message;
    private String htmlContent;

    public InternetAddress getSender() {
        return sender;
    }

    public void setSender(InternetAddress sender) {
        this.sender = sender;
    }

    public List<InternetAddress> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<InternetAddress> recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }
}
