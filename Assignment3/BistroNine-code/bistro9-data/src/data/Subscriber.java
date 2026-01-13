package data;

/**
 * Entity class representing a Subscriber in the system.
 * This class extends {@link Customer}, adding authentication details, 
 * personal information, and specific user roles (type).
 */
public class Subscriber extends Customer
{
	private int subscriberId;//number of subscriber,give by DB auto increment
	// customerId;   //number of customer ,give by DB auto increment
	private String firstName;
	private String lastName;
	private String type;//'subscriber', 'restaurant representative', 'restaurant manager
	private String personalInfo;
	private String username;
	private String password;
 	

	/**
	 * Default constructor for Subscriber.
	 * Initializes a new instance and calls the parent {@link Customer} constructor.
	 */
	public Subscriber()
	{
		super();
	}

	/**
	 * Gets the unique subscriber identifier.
	 * @return The subscriber ID.
	 */
	public int getSubscriberId() {
		return subscriberId;
	}

	/**
	 * Sets the unique subscriber identifier.
	 * @param subscriberId The ID to set.
	 */
	public void setSubscriberId(int subscriberId) {
		this.subscriberId = subscriberId;
	}
	
	/**
	 * Gets the personal information/bio of the subscriber.
	 * @return The personal info string.
	 */
	public String getPersonalInfo() {
		return personalInfo;
	}

	/**
	 * Sets the personal information/bio for the subscriber.
	 * @param personalInfo The info string to set.
	 */
	public void setPersonalInfo(String personalInfo) {
		this.personalInfo = personalInfo;
	}
	
	/**
	 * Gets the subscriber's username for system login.
	 * @return The username.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the subscriber's username for system login.
	 * @param username The username to set.
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
	/**
	 * Gets the subscriber's account password.
	 * @return The password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the subscriber's account password.
	 * @param password The password to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Gets the subscriber's first name.
	 * @return The first name.
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Sets the subscriber's first name.
	 * @param firstName The first name to set.
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Gets the subscriber's last name.
	 * @return The last name.
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * Sets the subscriber's last name.
	 * @param lastName The last name to set.
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * Gets the account type (e.g., 'subscriber', 'restaurant representative', 'restaurant manager').
	 * @return The type string.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the account type.
	 * @param type The role/type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}

}