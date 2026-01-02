package testsServer;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import controller.DataBaseController;
import controller.ReservationControler; 
import data.Command; 
import data.Message;
import data.TableReservation;

import testsRunners.TestConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationControllerTests {

    private ReservationControler reservationController;
    
    // Static variable to store the confirmation code between tests
    private static int createdReservationCode = 0;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        System.out.println("--- Setting up DB Connection for Reservation Tests ---");
        try {
            // 1. Initialize the DB Controller using the config password
            DataBaseController.initiateDBC(TestConfig.DB_PASSWORD);
            
            // 2. Ensure connection is alive by getting the instance
            DataBaseController db = DataBaseController.getInstance();
            if (db == null) {
                fail("DataBaseController.getInstance() returned null. Check DB connection/password.");
            }
            
            // 3. VERIFY CONNECTION
            // Attempt a read-only query to force a connection check immediately.
            System.out.print("Verifying DB connection... ");
            if (db.getAllReservationsQuery() == null) {
                System.out.println("FAILED.");
                fail("DB Connection Verification Failed! Query returned null. Check your password in TestConfig.java");
            }
            System.out.println("SUCCESS.");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to connect to DB: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Create a fresh instance of the controller logic for each test
        reservationController = new ReservationControler();
    }

    @Test
    @Order(1)
    void testCheckTableAvailability() {
        System.out.println("Test 1: Check Table Availability");
        
        // Prepare Message Content: [numberOfDiners (Integer), reservationDay (LocalDate)]
        ArrayList<Object> content = new ArrayList<>();
        content.add(4); // 4 diners
        content.add(LocalDate.now().plusDays(1)); // Check for tomorrow
        
        Message msg = new Message();
        msg.command = Command.CHECK_TABLE_AVAILABILITY;
        msg.content = content;
        
        // Act
        Object result = reservationController.handleMessageFromServer(msg);
        
        // Assert
        if (result != null) {
            assertTrue(result instanceof ArrayList, "Result should be an ArrayList of LocalTimes");
            System.out.println("Available times found: " + result.toString());
        } else {
            System.out.println("No available times (result is null), which is a valid logical result.");
        }
    }

    @Test
    @Order(2)
    void testCreateNewReservation() {
        System.out.println("Test 2: Create New Reservation");

        // Prepare Message Content
        ArrayList<Object> content = new ArrayList<>();
        content.add("customer"); 
        content.add("0509999999"); // Dummy Phone
        content.add("testuser@junit.com"); // Dummy Email
        content.add(2); // Diners
        
        // Set time for tomorrow at 19:00
        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atTime(19, 0);
        content.add(Timestamp.valueOf(tomorrow)); 

        Message msg = new Message();
        msg.command = Command.CREATE_NEW_RESERVATION;
        msg.content = content;

        // Act
        Object result = reservationController.handleMessageFromServer(msg);

        // Assert
        assertNotNull(result, "Creation failed, returned null. Check if DB has tables/customers.");
        assertTrue(result instanceof Integer, "Should return the Confirmation Code (Integer)");
        
        // Store code for the next test
        createdReservationCode = (Integer) result;
        System.out.println("Created Reservation Code: " + createdReservationCode);
    }
    
    @Test
    @Order(3)
    void testGetAllReservationsByCustomer() {
        System.out.println("Test 3: Get Reservations By Customer");
        
        // Input format: [type (String), phone (String), email (String)]
        ArrayList<Object> content = new ArrayList<>();
        content.add("customer");
        content.add("0509999999");
        content.add("testuser@junit.com");
        
        Message msg = new Message();
        msg.command = Command.GET_ALL_RESERVATIONS_BY_CUSTOMER;
        msg.content = content;
        
        // Act
        Object result = reservationController.handleMessageFromServer(msg);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ArrayList, "Should return a list of TableReservation objects");
        
        @SuppressWarnings("unchecked")
        ArrayList<TableReservation> list = (ArrayList<TableReservation>) result;
        
        // Verify we found the reservation from Test 2
        boolean found = false;
        for(TableReservation res : list) {
            if(res.getConfirmationCode() == createdReservationCode) {
                found = true;
                break;
            }
        }
        assertTrue(found, "The reservation created in Test 2 should appear in the customer's list");
    }

    @Test
    @Order(4)
    void testDeleteReservation() {
        System.out.println("Test 4: Delete Reservation");
        
        // Ensure Test 2 passed
        assertNotEquals(0, createdReservationCode, "Skipping delete test because creation failed or code is 0");

        // Input format: [confirmationCode (Integer)]
        ArrayList<Object> content = new ArrayList<>();
        content.add(createdReservationCode);
        
        Message msg = new Message();
        msg.command = Command.DELETE_RESERVATION;
        msg.content = content;
        
        // Act
        Object result = reservationController.handleMessageFromServer(msg);
        
        // Assert
        assertEquals(true, result, "Delete operation should return true");
        System.out.println("Reservation " + createdReservationCode + " deleted successfully.");
    }
}