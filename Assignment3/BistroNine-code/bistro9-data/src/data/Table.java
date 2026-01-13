package data;

/**
 * Entity class representing a physical table in the restaurant.
 * Tracks the table's capacity, physical location, and current availability status.
 */
public class Table 
{
	private int tableId  ;//give by DB auto increment
	private int seatsNumber ;
	private String location ; //inside, bar, outside
	private String status ; //available, occupied, deleted
   
	
	/**
	 * Default constructor for Table.
	 */
	public Table() 
	{
	}


	/**
	 * Gets the unique identifier for the table.
	 * @return The table ID.
	 */
	public int getTableId() {
		return tableId;
	}


	/**
	 * Sets the unique identifier for the table.
	 * @param tableId The ID to set.
	 */
	public void setTableId(int tableId) {
		this.tableId = tableId;
	}


	/**
	 * Gets the number of seats (capacity) of the table.
	 * @return The number of seats.
	 */
	public int getSeatsNumber() {
		return seatsNumber;
	}


	/**
	 * Sets the number of seats (capacity) for the table.
	 * @param seatsNumber The number of seats to set.
	 */
	public void setSeatsNumber(int seatsNumber) {
		this.seatsNumber = seatsNumber;
	}


	/**
	 * Gets the physical location of the table (e.g., "inside", "bar", "outside").
	 * @return The location description.
	 */
	public String getLocation() {
		return location;
	}


	/**
	 * Sets the physical location of the table.
	 * @param location The location string to set.
	 */
	public void setLocation(String location) {
		this.location = location;
	}


	/**
	 * Gets the current status of the table (e.g., "available", "occupied", "deleted").
	 * @return The current status.
	 */
	public String getStatus() {
		return status;
	}


	/**
	 * Sets the status of the table.
	 * @param status The status string to set.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

}