package controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data.Command;
import data.Message;

class WaitListControllerTest {

	private static DataBaseController dbc;
	private WaitListController waitListController;
	
	// Store IDs to clean up database after tests
	private ArrayList<Integer> confCodesToDelete; 

	@BeforeAll
	static void globalSetup() {
		// 1. Initialize the Database Connection
		// IMPORTANT: Change "1234" to your actual MySQL root password
		DataBaseController.initiateDBC("1234"); 
		dbc = DataBaseController.getInstance();
	}

	@BeforeEach
	void setUp() throws Exception {
		// 2. Initialize the Controller
		waitListController = new WaitListController();
		confCodesToDelete = new ArrayList<>();
	}

	@AfterEach
	void tearDown() {
		// 3. Clean up: Delete any reservations created during the test
		if (!confCodesToDelete.isEmpty()) {
			for (Integer code : confCodesToDelete) {
				System.out.println("Cleaning up test data: Deleting reservation " + code);
				dbc.deleteReservationByConfCode(code);
			}
		}
	}

	// Helper method to create Message objects easily
	private Message createMessage(Command command, Object content) {
		Message msg = new Message();
		msg.command = command;
		msg.content = content;
		return msg;
	}

	/**
	 * Test 1: Add a Walk-In Customer to the WaitList.
	 * Checks if the system returns a valid Confirmation Code.
	 */
	@Test
	void testGetInToWaitList_WalkIn() {
		// Arrange
		ArrayList<Object> content = new ArrayList<>();
		content.add("customer");           // Type
		content.add("050-999TEST");        // Phone
		content.add("test@unit.test");     // Email
		content.add(15);                   // Large number of diners to force WaitList

		Message msg = createMessage(Command.GET_IN_TO_WAIT_LIST, content);

		// Act
		Object result = waitListController.handleMessageFromServer(msg);

		// Assert
		assertNotNull(result, "Result should not be null");
		assertTrue(result instanceof Integer, "Result should be an Integer (Confirmation Code)");

		int code = (Integer) result;
		assertTrue(code > 0, "Should return a positive confirmation code for waitlist");
		
		confCodesToDelete.add(code); // Mark for cleanup
		
		// Verify it actually exists in the DB
		assertTrue(dbc.checkIfConfCodeExistsInDB(code));
		assertTrue(dbc.checkIfConfCodeExistsInWaitingList(code));
	}

	/**
	 * Test 2: Get the list of people waiting.
	 * Verifies that the ManWaiting object contains the correct phone number.
	 */
	@Test
	void testGetWaitList() {
		// 1. Arrange: Insert a customer first
		ArrayList<Object> content = new ArrayList<>();
		content.add("customer");
		content.add("050-888TEST");
		content.add("waitlist@test.com");
		content.add(20); 
		
		Message insertMsg = createMessage(Command.GET_IN_TO_WAIT_LIST, content);
		Integer code = (Integer) waitListController.handleMessageFromServer(insertMsg);
		confCodesToDelete.add(code); // Mark for cleanup

		// 2. Act: Request the wait list
		Message getMsg = createMessage(Command.GET_WAIT_LIST, null);
		Object result = waitListController.handleMessageFromServer(getMsg);

		// 3. Assert
		assertNotNull(result);
		assertTrue(result instanceof ArrayList<?>);
		
		@SuppressWarnings("unchecked")
		ArrayList<ManWaiting> waitingList = (ArrayList<ManWaiting>) result;
		
		assertFalse(waitingList.isEmpty(), "List should not be empty");
		
		// Verify the specific customer is in the list using ManWaiting methods
		boolean found = false;
		for(ManWaiting man : waitingList) {
			if(man.getPhoneNumber().equals("050-888TEST")) {
				found = true;
				// Optional: Check other fields
				assertEquals("waitlist@test.com", man.getEmail());
				break;
			}
		}
		assertTrue(found, "The customer with phone 050-888TEST should be in the list");
	}

	/**
	 * Test 3: Delete a customer from the WaitList.
	 * Verifies that the deletion logic works and returns true.
	 */
	@Test
	void testDeleteFromWaitList() {
		// 1. Arrange: Insert a customer
		ArrayList<Object> insertContent = new ArrayList<>();
		insertContent.add("customer");
		insertContent.add("050-777TEST");
		insertContent.add("delete@test.com");
		insertContent.add(20); 
		
		Message insertMsg = createMessage(Command.GET_IN_TO_WAIT_LIST, insertContent);
		Integer code = (Integer) waitListController.handleMessageFromServer(insertMsg);
		confCodesToDelete.add(code); 
		
		// 2. Arrange: Prepare delete message
		ArrayList<Object> deleteContent = new ArrayList<>();
		deleteContent.add("customer");
		deleteContent.add("050-777TEST");
		deleteContent.add("delete@test.com");
		
		Message deleteMsg = createMessage(Command.DELETE_FROM_WAIT_LIST, deleteContent);
		
		// 3. Act
		Object result = waitListController.handleMessageFromServer(deleteMsg);
		
		// 4. Assert
		assertNotNull(result);
		assertTrue((Boolean) result, "Delete operation should return true");
		
		// 5. Verify they are gone from the active list
		Message getListMsg = createMessage(Command.GET_WAIT_LIST, null);
		@SuppressWarnings("unchecked")
		ArrayList<ManWaiting> currentList = (ArrayList<ManWaiting>) waitListController.handleMessageFromServer(getListMsg);
		
		boolean found = false;
		if (currentList != null) {
			for(ManWaiting man : currentList) {
				if(man.getPhoneNumber().equals("050-777TEST")) {
					found = true;
					break;
				}
			}
		}
		assertFalse(found, "Deleted customer should no longer appear in the active wait list");
	}
}