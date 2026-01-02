package testsServer;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import controller.BillController;
import controller.DataBaseController;
import data.Bill;
import data.Command;
import data.Message;
import data.Subscriber;
import data.Table;
import data.TableReservation;
import testsRunners.TestConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BillControllerTests {

    private BillController billController;

    // Static variables to pass data between test steps
    private static int storedReservationId = 0;
    private static int storedConfirmationCode = 0;
    private static int storedCustomerId = 0;
    private static int storedTableId = 0; 

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        System.out.println("--- Setting up DB Connection for Bill Tests ---");
        try {
            // 1. Initialize DB
            DataBaseController.initiateDBC(TestConfig.DB_PASSWORD);
            DataBaseController db = DataBaseController.getInstance();
            
            if (db == null) {
                fail("DB Connection Failed. Check TestConfig.java");
            }
            
            // 2. Verify Connection
            if(db.getAllSubscribersQuery() == null) {
                fail("DB Connection failed (query returned null). Check password.");
            }
            
            System.out.println("Connection Verified.");
            
            // 3. Create a Dummy Table
            // We need a valid tableId for the reservation Foreign Key constraint.
            Table table = new Table();
            table.setSeatsNumber(4); 
            table.setLocation("inside");
            table.setStatus("available");
            
            Table createdTable = db.addTableQuery(table);
            if (createdTable != null && createdTable.getTableId() > 0) {
                storedTableId = createdTable.getTableId();
                System.out.println("Setup Table Created. ID: " + storedTableId);
            } else {
                // Fallback if addTableQuery fails or isn't implemented
                System.out.println("Warning: Could not create setup table. Defaulting to ID 1.");
                storedTableId = 1; 
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Setup failed: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        billController = new BillController();
    }

    @Test
    @Order(1)
    void testCreateNewBill() {
        System.out.println("Test 1: Create New Bill (Logic)");

        try {
            // Step A: Create a UNIQUE Customer
            Subscriber sub = new Subscriber();
            sub.setFirstName("Bill");
            sub.setLastName("Tester");
            
            // Fix: Must match ENUM in DB
            sub.setType("subscriber"); 
            
            sub.setPersonalInfo("Safe Test");
            
            // Unique fields to avoid DB errors
            sub.setUsername("billUser_" + System.currentTimeMillis());
            sub.setPassword("pass");
            sub.setPhoneNumber("055" + (System.currentTimeMillis() % 10000000));
            sub.setEmail("safe_" + System.currentTimeMillis() + "@test.com");
            
            // --- FIX IS HERE ---
            // 1. Get/Create the Customer and CAPTURE the ID
            int custId = DataBaseController.getInstance().getCustomerId(sub);
            sub.setCustomerId(custId); // Update the object!
            
            // 2. Now add the Subscriber (now that customerId is set)
            int subId = DataBaseController.getInstance().addNewSubscriber(sub);
            sub.setSubscriberId(subId);
            
            storedCustomerId = custId; 

            // Step B: Create a Reservation
            TableReservation res = new TableReservation();
            res.setCustomerId(storedCustomerId);
            res.setTableId(storedTableId); 
            res.setNumberOfDiners(2);
            res.setReservationDate(Timestamp.valueOf(LocalDateTime.now()));
            res.setStatus("arrived"); 
            
            int code = 100000 + new Random().nextInt(900000);
            res.setConfirmationCode(code);
            storedConfirmationCode = code;

            // Insert reservation
            boolean resCreated = DataBaseController.getInstance().createNewReservation(res);
            assertTrue(resCreated, "Failed to create reservation in DB");
            
            // Retrieve the ID
            TableReservation searchRes = new TableReservation();
            searchRes.setConfirmationCode(code);
            DataBaseController.getInstance().getReservationsByConferenceCodeQuery(searchRes);
            storedReservationId = searchRes.getReservationId();

            System.out.println("Reservation Created. ResID: " + storedReservationId);

            // --- THE ACTUAL TEST ---
            boolean result = BillController.createNewBill(searchRes);
            
            assertTrue(result, "createNewBill should return true");
            System.out.println("Bill generated successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test 1 Failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    void testShowBill() {
        System.out.println("Test 2: Show Bill (Message)");

        // Prepare Message: [confirmation code (int)]
        ArrayList<Object> content = new ArrayList<>();
        content.add(storedConfirmationCode);

        Message msg = new Message();
        msg.command = Command.SHOW_BILL;
        msg.content = content;

        // Act
        // This only reads from DB. No emails.
        Object result = billController.handleMessageFromServer(msg);

        // Assert
        assertNotNull(result, "Should return a Bill object");
        assertTrue(result instanceof Bill, "Result should be of type Bill");

        Bill bill = (Bill) result;
        assertEquals(storedReservationId, bill.getReservationId());
        
        System.out.println("Bill retrieved successfully. ID: " + bill.getBillId());
    }
}