package com.plagiarism.detector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

	@Autowired
	private JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String fromEmail;

	@Value("${frontend.url}")
	private String frontendUrl;

	public void sendPasswordResetEmail(String toEmail, String username, String resetToken) throws MessagingException {

		String resetUrl = frontendUrl + "/reset-password/" + resetToken;

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

		helper.setFrom(fromEmail);
		helper.setTo(toEmail);
		helper.setSubject("Password Reset Request - AI Code Plagiarism Detector");

		String htmlContent = buildPasswordResetEmailTemplate(username, resetUrl);
		helper.setText(htmlContent, true);

		mailSender.send(message);
	}

	public void sendPasswordResetConfirmationEmail(String toEmail, String username) throws MessagingException {

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

		helper.setFrom(fromEmail);
		helper.setTo(toEmail);
		helper.setSubject("Password Reset Successful - AI Code Plagiarism Detector");

		String htmlContent = buildPasswordResetConfirmationTemplate(username);
		helper.setText(htmlContent, true);

		mailSender.send(message);
	}

	private String buildPasswordResetEmailTemplate(String username, String resetUrl) {
		return "<!DOCTYPE html>" + "<html>" + "<head>" + "<style>"
				+ "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
				+ ".container { max-width: 600px; margin: 0 auto; }"
				+ ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); "
				+ "color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }"
				+ ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }"
				+ ".button { display: inline-block; padding: 12px 30px; background: #667eea; "
				+ "color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }"
				+ ".footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }"
				+ ".warning { background: #fff3cd; border-left: 4px solid #ffc107; "
				+ "padding: 15px; margin: 20px 0; }" + "</style>" + "</head>" + "<body>" + "<div class='container'>"
				+ "<div class='header'>" + "<h1>Password Reset Request</h1>" + "</div>" + "<div class='content'>"
				+ "<p>Hello " + username + ",</p>"
				+ "<p>We received a request to reset your password for your AI Code Plagiarism Detector account.</p>"
				+ "<p>Click the button below to reset your password:</p>" + "<center>" + "<a href='" + resetUrl
				+ "' class='button'>Reset Password</a>" + "</center>"
				+ "<p>Or copy and paste this link into your browser:</p>"
				+ "<p style='word-break: break-all; color: #667eea;'>" + resetUrl + "</p>" + "<div class='warning'>"
				+ "<strong>⚠️ Important:</strong>" + "<ul>" + "<li>This link will expire in 1 hour</li>"
				+ "<li>If you didn't request this reset, please ignore this email</li>"
				+ "<li>Your password will remain unchanged until you create a new one</li>" + "</ul>" + "</div>"
				+ "<p>For security reasons, never share this link with anyone.</p>"
				+ "<p>Best regards,<br>AI Code Plagiarism Detector Team</p>" + "</div>" + "<div class='footer'>"
				+ "<p>This is an automated email. Please do not reply to this message.</p>" + "</div>" + "</div>"
				+ "</body>" + "</html>";
	}

	private String buildPasswordResetConfirmationTemplate(String username) {
		return "<!DOCTYPE html>" + "<html>" + "<head>" + "<style>"
				+ "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
				+ ".container { max-width: 600px; margin: 0 auto; }"
				+ ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); "
				+ "color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }"
				+ ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }" + "</style>"
				+ "</head>" + "<body>" + "<div class='container'>" + "<div class='header'>"
				+ "<h1>Password Reset Successful</h1>" + "</div>" + "<div class='content'>" + "<p>Hello " + username
				+ ",</p>" + "<p>Your password has been successfully reset.</p>"
				+ "<p>If you did not make this change, please contact our support team immediately.</p>"
				+ "<p>Best regards,<br>AI Code Plagiarism Detector Team</p>" + "</div>" + "</div>" + "</body>"
				+ "</html>";
	}
}