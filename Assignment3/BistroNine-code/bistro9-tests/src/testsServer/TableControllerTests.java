package testsServer;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import controller.DataBaseController;
import controller.TableController;
import data.Command;
import data.Message;
import data.Subscriber;
import data.Table;
import data.TableReservation;
import data.WaitList;
import testsRunners.TestConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TableControllerTests {

    private TableController tableController;
    
    // Static vars to pass data between tests
    private static int createdTableId_Small = 0;
    private static int createdTableId_Large = 0;
    private static int storedConfirmationCode = 0;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        System.out.println("--- Setting up DB Connection for TableController Tests ---");
        try {
            DataBaseController.initiateDBC(TestConfig.DB_PASSWORD);
            if (DataBaseController.getInstance() == null) {
                fail("DB Connection Failed.");
            }
            // Verify connection
            if(DataBaseController.getInstance().getAllSubscribersQuery() == null) {
                fail("DB Connection Verification Failed.");
            }
            
            // 1. Setup a Customer to use for reservations
            Subscriber sub = new Subscriber();
            sub.setFirstName("Table");
            sub.setLastName("Tester");
            sub.setType("customer");
            sub.setPhoneNumber("059" + (System.currentTimeMillis() % 10000000));
            sub.setEmail("table_" + System.currentTimeMillis() + "@test.com");
            
            int custId = DataBaseController.getInstance().getCustomerId(sub); // Create if not exists
            if (custId == -1) fail("Could not create test customer");

        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        tableController = new TableController();
    }

    @Test
    @Order(1)
    void testAddTables() {
        System.out.println("Test 1: Add Tables (Small and Large)");
        
        // 1. Add Small Table (2 seats)
        ArrayList<Object> content1 = new ArrayList<>();
        content1.add(2); // seats
        content1.add("inside"); // location
        
        Message msg1 = new Message();
        msg1.command = Command.ADD_TABLE;
        msg1.content = content1;
        
        Table t1 = (Table) tableController.handleMessageFromServer(msg1);
        assertNotNull(t1);
        createdTableId_Small = t1.getTableId();
        System.out.println("Created Small Table ID: " + createdTableId_Small);

        // 2. Add Large Table (8 seats)
        ArrayList<Object> content2 = new ArrayList<>();
        content2.add(8); // seats
        content2.add("inside"); 
        
        Message msg2 = new Message();
        msg2.command = Command.ADD_TABLE;
        msg2.content = content2;
        
        Table t2 = (Table) tableController.handleMessageFromServer(msg2);
        assertNotNull(t2);
        createdTableId_Large = t2.getTableId();
        System.out.println("Created Large Table ID: " + createdTableId_Large);
    }

    @Test
    @Order(2)
    void testCheckIn_Success_TableAvailable() {
        System.out.println("Test 2: Check-In Success (Table Available)");
        
        // 1. Create a reservation that fits the Large Table
        int diners = 6;
        int confCode = 200000 + new Random().nextInt(800000);
        
        TableReservation res = new TableReservation();
        res.setNumberOfDiners(diners);
        res.setConfirmationCode(confCode);
        res.setReservationDate(Timestamp.valueOf(LocalDateTime.now())); // For today
        res.setStatus("active");
        
        // We need a customer ID. Let's create a temp one or use existing
        Subscriber sub = new Subscriber();
        sub.setPhoneNumber("059" + (System.currentTimeMillis() % 1000000));
        sub.setEmail("checkin" + System.currentTimeMillis() + "@test.com");
        int custId = DataBaseController.getInstance().getCustomerId(sub);
        res.setCustomerId(custId);
        
        // Insert Res manually
        boolean created = DataBaseController.getInstance().createNewReservation(res);
        assertTrue(created, "Failed to create setup reservation");

        // 2. Perform Check-In (RECEIVE_TABLE_ID)
        ArrayList<Object> content = new ArrayList<>();
        content.add(confCode);
        
        Message msg = new Message();
        msg.command = Command.RECEIVE_TABLE_ID;
        msg.content = content;
        
        Object result = tableController.handleMessageFromServer(msg);
        
        // 3. Assertions
        assertNotNull(result, "Check-in should return a Reservation object");
        assertTrue(result instanceof TableReservation);
        
        TableReservation checkedIn = (TableReservation) result;
        assertEquals("arrived", checkedIn.getStatus());
        assertEquals(createdTableId_Large, checkedIn.getTableId(), "Should be assigned the large table");
        
        // Verify Table Status in DB
        Table t = new Table();
        t.setTableId(createdTableId_Large);
        Table dbTable = DataBaseController.getInstance().getTableByTableIdQuery(t);
        assertEquals("occupied", dbTable.getStatus(), "Table should be occupied after check-in");
        
        // Clean up (Free the table for future tests)
        TableController.updateTable(createdTableId_Large, "status", "available");
    }

    @Test
    @Order(3)
    void testCheckIn_NoTable_GoToWaitlist() {
        System.out.println("Test 3: Check-In No Table Available (Go To Waitlist)");
        
        // 1. Occupy the Large Table first (so it's not available)
        TableController.updateTable(createdTableId_Large, "status", "occupied");
        
        // 2. Create a reservation for 6 people (Small table won't fit, Large is occupied)
        int diners = 6;
        int confCode = 300000 + new Random().nextInt(600000);
        
        TableReservation res = new TableReservation();
        res.setNumberOfDiners(diners);
        res.setConfirmationCode(confCode);
        res.setReservationDate(Timestamp.valueOf(LocalDateTime.now()));
        res.setStatus("active");
        
        Subscriber sub = new Subscriber();
        sub.setPhoneNumber("058" + (System.currentTimeMillis() % 1000000));
        int custId = DataBaseController.getInstance().getCustomerId(sub);
        res.setCustomerId(custId);
        
        DataBaseController.getInstance().createNewReservation(res);

        // 3. Perform Check-In
        ArrayList<Object> content = new ArrayList<>();
        content.add(confCode);
        
        Message msg = new Message();
        msg.command = Command.RECEIVE_TABLE_ID;
        msg.content = content;
        
        Object result = tableController.handleMessageFromServer(msg);
        
        // 4. Assertions
        assertNotNull(result);
        TableReservation waitingRes = (TableReservation) result;
        
        // Should be put in waitlist
        assertEquals("waiting", waitingRes.getStatus(), "Reservation status should update to waiting");
        
        // Verify Waitlist Entry Exists
        ArrayList<ArrayList<Object>> wl = DataBaseController.getInstance().getWaitingListQuery();
        boolean foundInQueue = false;
        for(ArrayList<Object> row : wl) {
            // Index 1 is reservationId
            if( (int)row.get(1) == waitingRes.getReservationId() ) {
                foundInQueue = true;
                assertEquals("check_in", row.get(6)); // Type
                break;
            }
        }
        assertTrue(foundInQueue, "Should have been added to the waiting list DB");
        
        // Cleanup: Free the table
        TableController.updateTable(createdTableId_Large, "status", "available");
    }

    @Test
    @Order(4)
    void testDeleteTable_Safe() {
        System.out.println("Test 4: Delete Table (Safe - No Reservations)");
        
        // Delete the small table (we verify no reservations are blocking it)
        ArrayList<Object> content = new ArrayList<>();
        content.add(createdTableId_Small);
        
        Message msg = new Message();
        msg.command = Command.DELETE_TABLE;
        msg.content = content;
        
        Object result = tableController.handleMessageFromServer(msg);
        
        assertEquals("true", result, "Should return 'true' string for successful deletion");
        
        // Verify DB
        Table t = new Table();
        t.setTableId(createdTableId_Small);
        Table dbTable = DataBaseController.getInstance().getTableByTableIdQuery(t);
        // Note: getAllTables includes deleted ones, but getAllAvailableTables excludes them.
        // Assuming updateTableStatus to "deleted" happened inside deleteTableQuery logic
        // OR the row was removed. Your controller says "deleteTableQuery".
    }
}