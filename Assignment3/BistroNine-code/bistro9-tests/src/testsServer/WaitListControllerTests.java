package testsServer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import controller.DataBaseController;
import controller.WaitListController;
import data.Command;
import data.Message;
import data.Table;
import testsRunners.TestConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WaitListControllerTests {

    private WaitListController waitListController;
    
    // We use a specific huge table size for immediate seating to avoid conflicts with other tests
    private static final int IMMEDIATE_SEATS = 45; 
    // We use an impossible size to force the waitlist logic
    private static final int IMPOSSIBLE_SEATS = 999;
    
    // Store customer details for deletion tests
    private static String waitlistPhone = "059" + System.currentTimeMillis();

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        System.out.println("--- Setting up DB Connection for WaitList Tests ---");
        try {
            DataBaseController.initiateDBC(TestConfig.DB_PASSWORD);
            if (DataBaseController.getInstance() == null || DataBaseController.getInstance().getAllSubscribersQuery() == null) {
                fail("DB Connection Failed.");
            }
            
            // --- SETUP: Create Tables for Immediate Seating ---
            // We create multiple tables of this unique size to ensure "Available > Reservations" 
            // logic in findBestTableForNow returns true.
            for(int i=0; i<3; i++) {
                Table t = new Table();
                t.setSeatsNumber(IMMEDIATE_SEATS); 
                t.setLocation("inside");
                t.setStatus("available");
                DataBaseController.getInstance().addTableQuery(t);
            }
            System.out.println("Created 3 tables of size " + IMMEDIATE_SEATS + " for immediate seating tests.");
            
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        waitListController = new WaitListController();
    }

    @Test
    @Order(1)
    void testWalkIn_ImmediateSeating() {
        System.out.println("Test 1: Walk-In (Table Available -> Immediate Seating)");

        // Message: [type, phone, email, diners]
        ArrayList<Object> content = new ArrayList<>();
        content.add("customer");
        content.add("054" + System.currentTimeMillis()); // Random phone
        content.add(null); 
        content.add(IMMEDIATE_SEATS); // Requesting the specific size we created
        
        Message msg = new Message();
        msg.command = Command.GET_IN_TO_WAIT_LIST;
        msg.content = content;
        
        Object result = waitListController.handleMessageFromServer(msg);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof Integer, "Result should be an Integer");
        
        int response = (Integer) result;
        
        // Immediate seating returns negative Table ID
        assertTrue(response < 0, "Should return negative ID for immediate seating. Got: " + response);
        System.out.println("Seated immediately at table ID: " + Math.abs(response));
    }

    @Test
    @Order(2)
    void testWalkIn_NoTable_AddToQueue() {
        System.out.println("Test 2: Walk-In (No Table -> Add to Queue)");
        
        // Request a table size that DOES NOT EXIST (999) to force the waitlist.
        ArrayList<Object> content = new ArrayList<>();
        content.add("customer");
        content.add(waitlistPhone); // Save this phone for Test 4 (Delete)
        content.add("waitlist_test@test.com");
        content.add(IMPOSSIBLE_SEATS); 
        
        Message msg = new Message();
        msg.command = Command.GET_IN_TO_WAIT_LIST;
        msg.content = content;
        
        Object result = waitListController.handleMessageFromServer(msg);
        
        assertNotNull(result);
        assertTrue(result instanceof Integer);
        
        int response = (Integer) result;
        
        // Added to queue returns positive Confirmation Code
        assertTrue(response > 0, "Should return positive confirmation code for waitlist. Got: " + response);
        
        System.out.println("Added to waitlist with code: " + response);
        
        // Verify DB Entry exists
        ArrayList<ArrayList<Object>> wl = DataBaseController.getInstance().getWaitingListQuery();
        boolean found = false;
        // The query returns list of lists. Index 2 is diners, Index 6 is type.
        for(ArrayList<Object> row : wl) {
            if((int)row.get(2) == IMPOSSIBLE_SEATS && "walk_in".equals(row.get(6))) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Should find the walk-in entry in the database");
    }
    
    @Test
    @Order(3)
    void testGetWaitList() {
        System.out.println("Test 3: Get Wait List");

        Message msg = new Message();
        msg.command = Command.GET_WAIT_LIST;
        msg.content = new ArrayList<>(); // No content needed

        Object result = waitListController.handleMessageFromServer(msg);

        assertNotNull(result);
        assertTrue(result instanceof ArrayList, "Should return an ArrayList");
        
        ArrayList<?> list = (ArrayList<?>) result;
        // Since we added someone in Test 2, the list should not be empty
        assertFalse(list.isEmpty(), "Waitlist should not be empty (Test 2 added someone)");
        
        System.out.println("Waitlist retrieved successfully. Size: " + list.size());
    }

    @Test
    @Order(4)
    void testDeleteFromWaitList() {
        System.out.println("Test 4: Delete From Wait List");

        // Message: [type, phone, email]
        // We use the phone number we used in Test 2
        ArrayList<Object> content = new ArrayList<>();
        content.add("customer");
        content.add(waitlistPhone);
        content.add("waitlist_test@test.com"); 

        Message msg = new Message();
        msg.command = Command.DELETE_FROM_WAIT_LIST;
        msg.content = content;

        Object result = waitListController.handleMessageFromServer(msg);

        assertNotNull(result);
        assertTrue(result instanceof Boolean);
        assertTrue((Boolean) result, "Should return true for successful deletion");
        
        System.out.println("Successfully removed customer from waitlist.");
        
        // Verify they are gone (or status changed)
        // Ideally we check DB, but relying on the boolean return is standard for this level of testing.
    }
}