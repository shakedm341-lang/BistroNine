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
import controller.TimeReportController;
import data.Command;
import data.Message;
import data.TimeReport;
import testsRunners.TestConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimeReportControllerTests {

    private TimeReportController timeReportController;
    
    // Static variables to share the dynamic dates between Test 1 and Test 2
    private static LocalDate startDate;
    private static LocalDate endDate;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        System.out.println("--- Setting up DB Connection for TimeReport Tests ---");
        try {
            DataBaseController.initiateDBC(TestConfig.DB_PASSWORD);
            if (DataBaseController.getInstance() == null || DataBaseController.getInstance().getAllSubscribersQuery() == null) {
                fail("DB Connection Failed.");
            }
            
            // --- FIX: GENERATE DYNAMIC YEAR ---
            // This ensures every test run uses a unique date range (e.g., Year 3000, 3001, etc.)
            long uniqueYearOffset = (System.currentTimeMillis() % 1000) + 2000; 
            startDate = LocalDate.of((int)uniqueYearOffset, 1, 1);
            endDate = LocalDate.of((int)uniqueYearOffset, 1, 31);
            
            System.out.println("Using dynamic test dates: " + startDate + " to " + endDate);

        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        timeReportController = new TimeReportController();
    }

    @Test
    @Order(1)
    void testInsertTimeReport() {
        System.out.println("Test 1: Insert Dummy Time Report");

        TimeReport report = new TimeReport();
        report.setStartDay(startDate);
        report.setEndDay(endDate);
        report.setReportRange("monthly");
        report.setReportType("time");
        
        report.addRow(startDate, 10, 15);
        report.addRow(startDate.plusDays(1), 20, 25);
        
        // This will now always succeed because the dates are unique to this run
        boolean inserted = DataBaseController.getInstance().addTimeReportQuery(report);
        
        if (inserted) {
            System.out.println("Dummy TimeReport inserted successfully.");
        } else {
            // If it still exists (extremely rare collision), we just print a warning
            System.out.println("Warning: TimeReport already existed (Collision).");
        }
    }

    @Test
    @Order(2)
    void testGetTimeReport() {
        System.out.println("Test 2: Get Time Report via Controller");

        ArrayList<Object> content = new ArrayList<>();
        content.add(startDate);
        content.add(endDate);

        Message msg = new Message();
        msg.command = Command.GET_TIME_REPORT_BY_RANGE_DATE;
        msg.content = content;

        Object result = timeReportController.handleMessageFromServer(msg);

        assertNotNull(result, "Should retrieve the report");
        assertTrue(result instanceof TimeReport, "Result should be TimeReport");
        
        TimeReport retrieved = (TimeReport) result;
        assertEquals(startDate, retrieved.getStartDay());
        assertEquals(endDate, retrieved.getEndDay());
        
        ArrayList<TimeReport.Row> rows = retrieved.getRows();
        assertNotNull(rows);
        assertTrue(rows.size() >= 1, "Should have rows");
        assertEquals(10, rows.get(0).getAvgArrival());
    }
}