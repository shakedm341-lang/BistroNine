package data;

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
 	

	public Subscriber()
	{
		super();
	}

	public int getSubscriberId() {
		return subscriberId;
	}
	public void setSubscriberId(int subscriberId) {
		this.subscriberId = subscriberId;
	}
	
	public String getPersonalInfo() {
		return personalInfo;
	}
	public void setPersonalInfo(String personalInfo) {
		this.personalInfo = personalInfo;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
