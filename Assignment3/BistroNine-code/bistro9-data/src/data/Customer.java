package data;

public class Customer 
{
	private int id;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private String email;
	private String type;
	
	public Customer() 
	{
		
	}

	public int getId() {
		return id;
	}

	public void setId(int id) 
	{
		this.id = id;
	}
	public String getfirstName() {
		return firstName;
	}
	public void setfirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getlastName() {
		return lastName;
	}

	public void setlastName(String lastName) {
		this.lastName = lastName;
	}
	public String getPhoneNum() {
		return phoneNumber;
	}

	public void setphoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
