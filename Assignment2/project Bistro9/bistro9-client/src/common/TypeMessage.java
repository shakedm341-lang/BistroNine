package common;

/**
 * This enum defines the different categories (topics) of messages 
 * exchanged between the Client and the Server.
 * <p>
 * Using this enum allows the controllers to identify the domain of the 
 * requested operation (e.g., whether it's a reservation request or a payment action).
 */

public enum TypeMessage {
	
	/**
     * Indicates operations related to creating, updating, or canceling table reservations.
     */
	reservation,
	
	/**
     * Indicates operations related to generating and viewing system reports.
     */
	report,
	
	/**
     * Indicates operations related to the process of receiving a table or checking in customers.
     */
	reciveTable,
	
	/**
     * Indicates general administrative operations for the bistro settings and users.
     */
	managementBistro,
	
	/**
     * Indicates operations regarding bill settlement and payment processing.
     */
	payMent,
	
	/**
     * Indicates operations for viewing or retrieving the roster of existing reservations.
     */
	reservationList; 

}
