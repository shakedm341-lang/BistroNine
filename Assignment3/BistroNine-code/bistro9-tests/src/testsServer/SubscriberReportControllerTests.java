package testsServer;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import controller.DataBaseController;
import controller.SubscriberReportController;
import data.Command;
import data.Message;
import data.SubscriberReport;
import testsRunners.TestConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SubscriberReportControllerTests {

    private SubscriberReportController subscriberReportController;
    
    // Static variables to share the dynamic dates between Test 1 and Test 2
    private static LocalDate startDate;
    private static LocalDate endDate;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        System.out.println("--- Setting up DB Connection for SubscriberReport Tests ---");
        try {
            DataBaseController.initiateDBC(TestConfig.DB_PASSWORD);
            if (DataBaseController.getInstance() == null || DataBaseController.getInstance().getAllSubscribersQuery() == null) {
                fail("DB Connection Failed.");
            }
            
            // --- FIX: GENERATE DYNAMIC YEAR ---
            // Use a different offset or just a random year to avoid conflicts
            long uniqueYearOffset = (System.currentTimeMillis() % 1000) + 3000; // Years 3000+
            startDate = LocalDate.of((int)uniqueYearOffset, 2, 1);
            endDate = LocalDate.of((int)uniqueYearOffset, 2, 28);
            
            System.out.println("Using dynamic test dates: " + startDate + " to " + endDate);

        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        subscriberReportController = new SubscriberReportController();
    }

    @Test
    @Order(1)
    void testInsertSubscriberReport() {
        System.out.println("Test 1: Insert Dummy Subscriber Report");

        SubscriberReport report = new SubscriberReport();
        report.setStartDay(startDate);
        report.setEndDay(endDate);
        report.setReportRange("monthly");
        report.setReportType("subscriber");
        
        report.addRow(startDate, 5, 2);
        report.addRow(startDate.plusDays(1), 8, 0);

        boolean inserted = DataBaseController.getInstance().addSubscriberReportQuery(report);
        
        if (inserted) {
            System.out.println("Dummy SubscriberReport inserted successfully.");
        } else {
            System.out.println("Warning: SubscriberReport already existed (Collision).");
        }
    }

    @Test
    @Order(2)
    void testGetSubscriberReport() {
        System.out.println("Test 2: Get Subscriber Report via Controller");

        ArrayList<Object> content = new ArrayList<>();
        content.add(startDate);
        content.add(endDate);

        Message msg = new Message();
        msg.command = Command.GET_SUBSCRIBER_REPORT_BY_RANGE_DATE;
        msg.content = content;

        Object result = subscriberReportController.handleMessageFromServer(msg);

        assertNotNull(result, "Should retrieve the report");
        assertTrue(result instanceof SubscriberReport, "Result should be SubscriberReport");
        
        SubscriberReport retrieved = (SubscriberReport) result;
        assertEquals(startDate, retrieved.getStartDay());
        assertEquals(endDate, retrieved.getEndDay());
        
        ArrayList<SubscriberReport.Row> rows = retrieved.getRows();
        assertNotNull(rows);
        assertTrue(rows.size() >= 1, "Should have rows");
        assertEquals(5, rows.get(0).getTotalReservations());
    }
}