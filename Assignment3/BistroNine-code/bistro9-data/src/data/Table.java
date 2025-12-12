package data;

public class Table 
{
	private int tableId  ;//give by DB auto increment
	private int seatsNumber ;
	private String location ; //inside, bar, outside
	private String status ; //available, reserved, occupied
   
	
	public Table() 
	{
	}


	public int getTableId() {
		return tableId;
	}


	public void setTableId(int tableId) {
		this.tableId = tableId;
	}


	public int getSeatsNumber() {
		return seatsNumber;
	}


	public void setSeatsNumber(int seatsNumber) {
		this.seatsNumber = seatsNumber;
	}


	public String getLocation() {
		return location;
	}


	public void setLocation(String location) {
		this.location = location;
	}


	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}

}
