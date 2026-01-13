package data;

/**
 * Entity class representing a generic Customer in the system.
 * Holds basic contact information and the unique database identifier.
 */
public class Customer 
{
	/** Unique identifier for the customer. Auto-incremented in the database. */
	private int customerId;//give by DB auto increment
	
	/** The customer's phone number. */
	private String phoneNumber;
	
	/** The customer's email address. */
	private String email;
		
	/**
	 * Default constructor.
	 * Initializes a new instance of Customer.
	 */
	public Customer() 
	{
		
	}

	/**
	 * Gets the customer's phone number.
	 * @return The phone number as a String.
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * Sets the customer's phone number.
	 * @param phoneNumber The phone number to set.
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/**
	 * Gets the customer's email address.
	 * @return The email address.
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the customer's email address.
	 * @param email The email to set.
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Gets the unique customer identifier.
	 * @return The customer ID.
	 */
	public int getCustomerId() {
		return customerId;
	}

	/**
	 * Sets the customerId.
	 * Typically used when retrieving an existing customer from the DB.
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}
	

}