package data;

import java.sql.Timestamp;

/**
 * Entity class representing a person waiting in the restaurant's digital queue.
 * Holds contact details and the timestamp of when they joined the waiting list.
 */
public class ManWaiting 
{

	private String firstName;
	private String lastName;
	private String phoneNumber;
	private String email;
	private Timestamp  entryTimeToList ;
	
	/**
	 * Default constructor for ManWaiting.
	 */
	public ManWaiting() 
	{
		
	}

	/**
	 * Gets the person's first name.
	 * @return The first name.
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Sets the person's first name.
	 * @param firstName The first name to set.
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Gets the person's last name.
	 * @return The last name.
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * Sets the person's last name.
	 * @param lastName The last name to set.
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * Gets the person's phone number.
	 * @return The phone number as a String.
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * Sets the person's phone number.
	 * @param phoneNumber The phone number to set.
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/**
	 * Gets the person's email address.
	 * @return The email address.
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the person's email address.
	 * @param email The email to set.
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Gets the timestamp of when the person was added to the waiting list.
	 * @return The entry time timestamp.
	 */
	public Timestamp getEntryTimeToList() {
		return entryTimeToList;
	}

	/**
	 * Sets the timestamp for when the person joins the waiting list.
	 * @param entryTimeToList The entry timestamp to set.
	 */
	public void setEntryTimeToList(Timestamp entryTimeToList) {
		this.entryTimeToList = entryTimeToList;
	}

}