package controller;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailSendController 
{

	//the email and password of the sender email account
	private static final String MY_EMAIL = "bistro9.2025@gmail.com";
	private static final String MY_PASSWORD = "qpcp uhxl aouc mqnb"; 

	/**
	 * Sends an email to the specified email with the given subject and content.
	 *
	 * @param email   The recipient's email address.
	 * @param subject The subject of the email.
	 * @param content The content/body of the email.
	 */
	public static void sendEmail(String email, String subject, String content) 
	{
		if (email == null || email.isEmpty()) {
			System.err.println("Invalid email address provided.");
			return;
		}
		//setup mail server
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		//get connection to the mail server
		Session session = Session.getInstance(props, new javax.mail.Authenticator() 
		{
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(MY_EMAIL, MY_PASSWORD);
			}
		});

		try {
			//create email message
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(MY_EMAIL));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));

			message.setSubject(subject);
			message.setText(content);

			//send the email to the customer
			System.out.println("Trying to send an email to: " + email);
			Transport.send(message);
			System.out.println("The email was sent successfully.!");

		} catch (MessagingException e) 
		{
			e.printStackTrace();
			System.err.println("Error sending email: " + e.getMessage());
		}
	}
}