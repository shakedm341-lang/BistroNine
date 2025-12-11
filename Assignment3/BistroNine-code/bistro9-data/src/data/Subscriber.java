package data;

public class Subscriber extends Customer
{
	private int subscriberId;
	private String personalInfo;
	private String username;
	private String password;

	public Subscriber()
	{
		super();
	}

	public int getsubscriberId() {
		return subscriberId;
	}
	public void setsubscriberId(int subscriberId) {
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

}
