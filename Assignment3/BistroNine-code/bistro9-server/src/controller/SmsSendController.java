package controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SmsSendController {

	/**
	 * Simulates sending an SMS to the specified phone number with the given
	 * message.
	 *
	 * @param phoneNumber The recipient's phone number.
	 * @param message     The message body of the SMS.
	 */
	public static void sendSms(String phoneNumber, String subject, String content) 
	{
		if (phoneNumber == null || phoneNumber.isEmpty()) {
			System.err.println("Invalid phone number provided.");
		}
		
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

		
		System.out.println("\n+===================================================+");
		System.out.println("|               📱 SEND SMS SIMULATION 📱             |");
		System.out.println("+===================================================+");
		System.out.println("| Time:   " + timestamp);
		System.out.println("| To:     " + phoneNumber);
		System.out.println("|---------------------------------------------------|");
		System.out.println("| Subject:                                          |");
		System.out.println("| " + subject);
		System.out.println("| Message Body:                                     |");
		System.out.println("| " + content);
		System.out.println("+===================================================+\n");
	}
}