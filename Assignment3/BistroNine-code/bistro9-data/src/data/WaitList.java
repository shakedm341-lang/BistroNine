package data;

import java.sql.Timestamp;

public class WaitList 
{

	private int waitingId ;// AUTO_INCREMENT in DB
	private int customerId ;
    private int numberOfDiners ;
    private int confirmationCode ;
    private Timestamp entryTime ;//DEFAULT CURRENT_TIMESTAMP in DB
    private String status ; //waiting, seated, cancelled //DEFAULT 'waiting', in DB
	
	public WaitList() 
	{
		// TODO Auto-generated constructor stub
	}

	public int getWaitingId() {
		return waitingId;
	}

	public void setWaitingId(int waiting_id) {
		this.waitingId = waiting_id;
	}

	public int getCustomerId() {
		return customerId;
	}

	public void setCustomerId(int customer_id) {
		this.customerId = customer_id;
	}

	public int getNumberOfDiners() {
		return numberOfDiners;
	}

	public void setNumberOfDiners(int numberOfDiners) {
		this.numberOfDiners = numberOfDiners;
	}

	public Timestamp getEntryTime() {
		return entryTime;
	}

	public void setEntryTime(Timestamp entry_time) {
		this.entryTime = entry_time;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	
	
	
}
