package testsServer;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import controller.DataBaseController;
import controller.OpeningTimeController;
import data.Command;
import data.Message;
import data.OpeningHours;
import data.OpeningHoursPerDay;
import data.TimeSlot;
import testsRunners.TestConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpeningTimeControllerTests {

    private OpeningTimeController openingTimeController;
    
    // Static variables to verify data persistence between tests
    private static final String TEST_DAY = "MONDAY";
    private static final LocalDate TEST_SPECIAL_DATE = LocalDate.of(2030, 1, 1); // Far future date

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        System.out.println("--- Setting up DB Connection for OpeningTime Tests ---");
        try {
            // 1. Initialize DB
            DataBaseController.initiateDBC(TestConfig.DB_PASSWORD);
            DataBaseController db = DataBaseController.getInstance();
            
            if (db == null) {
                fail("DB Connection Failed. Check TestConfig.java");
            }
            
            // 2. Verify Connection
            if(db.getAllSubscribersQuery() == null) {
                fail("DB Connection failed. Check password.");
            }
            System.out.println("Connection Verified.");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Setup failed: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        openingTimeController = new OpeningTimeController();
    }

    @Test
    @Order(1)
    void testUpdateWeeklyOpeningTime() {
        System.out.println("Test 1: Update Weekly Opening Time (" + TEST_DAY + ")");

        // Prepare Content: [String day, ArrayList<LocalTime> times]
        ArrayList<Object> content = new ArrayList<>();
        content.add(TEST_DAY);
        
        // Define new hours: 08:00-12:00 AND 14:00-18:00 (Split shift)
        ArrayList<LocalTime> hours = new ArrayList<>();
        hours.add(LocalTime.of(8, 0));  // Start 1
        hours.add(LocalTime.of(12, 0)); // End 1
        hours.add(LocalTime.of(14, 0)); // Start 2
        hours.add(LocalTime.of(18, 0)); // End 2
        content.add(hours);

        Message msg = new Message();
        msg.command = Command.UPDATE_OPENING_TIME;
        msg.content = content;

        // Act
        Object result = openingTimeController.handleMessageFromServer(msg);

        // Assert
        assertNotNull(result);
        assertTrue((Boolean) result, "Update should return true");
        System.out.println("Weekly hours updated successfully.");
    }

    @Test
    @Order(2)
    void testGetWeeklyOpeningTime() {
        System.out.println("Test 2: Get Weekly Opening Time");

        Message msg = new Message();
        msg.command = Command.GET_WEEKLY_OPENING_TIME;
        msg.content = new ArrayList<>(); // Content not used

        // Act
        Object result = openingTimeController.handleMessageFromServer(msg);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ArrayList, "Should return ArrayList<OpeningHours>");
        
        @SuppressWarnings("unchecked")
        ArrayList<OpeningHours> weekList = (ArrayList<OpeningHours>) result;
        assertEquals(7, weekList.size(), "Should return 7 days");
        
        // Verify MONDAY has the hours we set in Test 1
        boolean foundMonday = false;
        for (OpeningHours day : weekList) {
            if (day.getDay().equalsIgnoreCase(TEST_DAY)) {
                foundMonday = true;
                ArrayList<TimeSlot> slots = day.getSlots();
                assertNotNull(slots);
                // We added 2 slots (8-12, 14-18)
                assertTrue(slots.size() >= 1, "Monday should have slots");
                
                // --- FIX: Use getOpen() instead of getStartTime() ---
                assertEquals(LocalTime.of(8, 0), slots.get(0).getOpen());
                break;
            }
        }
        assertTrue(foundMonday, "Monday should be in the list");
    }

    @Test
    @Order(3)
    void testAddNewSpecialOpeningTime() {
        System.out.println("Test 3: Add Special Opening Time (" + TEST_SPECIAL_DATE + ")");

        // Content: [LocalDate date, ArrayList<LocalTime> times]
        ArrayList<Object> content = new ArrayList<>();
        content.add(TEST_SPECIAL_DATE);
        
        // Define special hours: 10:00 - 14:00
        ArrayList<LocalTime> hours = new ArrayList<>();
        hours.add(LocalTime.of(10, 0));
        hours.add(LocalTime.of(14, 0));
        content.add(hours);

        Message msg = new Message();
        msg.command = Command.ADD_NEW_SPECIAL_OPENING_TIME;
        msg.content = content;

        // Act
        Object result = openingTimeController.handleMessageFromServer(msg);

        // Assert
        assertTrue((Boolean) result, "Add Special Time should return true");
    }

    @Test
    @Order(4)
    void testGetSpecialOpeningTime() {
        System.out.println("Test 4: Get Special Opening Time");

        Message msg = new Message();
        msg.command = Command.GET_SPECIAL_OPENING_TIME;
        msg.content = new ArrayList<>();

        // Act
        Object result = openingTimeController.handleMessageFromServer(msg);

        // Assert
        if (result == null) {
            fail("Result is null. Ensure at least one special date exists (Test 3 should have added one).");
        }
        
        assertTrue(result instanceof ArrayList);
        @SuppressWarnings("unchecked")
        ArrayList<OpeningHoursPerDay> specialList = (ArrayList<OpeningHoursPerDay>) result;
        
        // Verify our 2030 date is in the list
        boolean foundDate = false;
        for (OpeningHoursPerDay special : specialList) {
            if (special.getDay().equals(TEST_SPECIAL_DATE)) {
                foundDate = true;
                // --- FIX: Use getOpen() instead of getStartTime() ---
                assertEquals(LocalTime.of(10, 0), special.getSlots().get(0).getOpen());
                break;
            }
        }
        assertTrue(foundDate, "The special date 2030-01-01 should be found.");
    }
}