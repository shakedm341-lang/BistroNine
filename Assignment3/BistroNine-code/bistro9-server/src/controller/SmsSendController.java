package controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SmsSendController {

    private static final int FRAME_WIDTH = 70;

    public static void sendSms(String phoneNumber, String subject, String content) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            System.err.println("Invalid phone number provided.");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

        System.out.println("\n+" + "=".repeat(FRAME_WIDTH - 2) + "+");
        printFrameLine("📱 SEND SMS SIMULATION 📱", true);
        System.out.println("+" + "=".repeat(FRAME_WIDTH - 2) + "+");
        
        printFrameLine("Time:   " + timestamp, false);
        printFrameLine("To:     " + phoneNumber, false);
        System.out.println("|" + "-".repeat(FRAME_WIDTH - 2) + "|");
        
        printFrameLine("Subject:", false);
        wrapAndPrint(subject);
        printFrameLine("", false); 
        
        printFrameLine("Message Body:", false);
        wrapAndPrint(content);
        
        System.out.println("+" + "=".repeat(FRAME_WIDTH - 2) + "+\n");
    }

    private static void wrapAndPrint(String text) {
        int maxWidth = FRAME_WIDTH - 4;
        
        // תיקון קריטי: פיצול לפי ירידות שורה וניקוי רווחים מיותרים
        String[] paragraphs = text.split("\\r?\\n");
        
        for (String paragraph : paragraphs) {
            String temp = paragraph.trim();
            if (temp.isEmpty()) {
                printFrameLine("", false);
                continue;
            }
            
            while (temp.length() > maxWidth) {
                int breakIndex = temp.lastIndexOf(' ', maxWidth);
                if (breakIndex == -1) breakIndex = maxWidth; 
                
                printFrameLine(temp.substring(0, breakIndex).trim(), false);
                temp = temp.substring(breakIndex).trim();
            }
            if (!temp.isEmpty()) {
                printFrameLine(temp, false);
            }
        }
    }

    private static void printFrameLine(String text, boolean center) {
        int contentWidth = FRAME_WIDTH - 4;
        // הסרת תווים שעלולים לשבור את השורה בטעות
        text = text.replace("\n", "").replace("\r", "");
        
        if (center) {
            int padding = Math.max(0, (contentWidth - text.length()) / 2);
            StringBuilder sb = new StringBuilder("| ");
            sb.append(" ".repeat(padding));
            sb.append(text);
            while (sb.length() < FRAME_WIDTH - 1) {
                sb.append(" ");
            }
            sb.append("|");
            System.out.println(sb.toString());
        } else {
            int spacesToAdd = Math.max(0, contentWidth - text.length());
            System.out.println("| " + text + " ".repeat(spacesToAdd) + " |");
        }
    }
}