package data;

import java.sql.Timestamp;

public class ManWaiting 
{

	private String firstName;
	private String lastName;
	private String phoneNumber;
	private String email;
	private Timestamp  entryTimeToList ;
	
	public ManWaiting() 
	{
		
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

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Timestamp getEntryTimeToList() {
		return entryTimeToList;
	}

	public void setEntryTimeToList(Timestamp entryTimeToList) {
		this.entryTimeToList = entryTimeToList;
	}

}
