package data;

import java.sql.Timestamp;

public class WaitList 
{
	private int waitingId ;// AUTO_INCREMENT in DB//
	private int reservationId;
	private int numberOfDiners;//
    private Timestamp  entryTimeToList ;//DEFAULT CURRENT_TIMESTAMP in DB//
    private Timestamp exitTimeFromList;
    private String status ; //waiting, seated, cancelled //DEFAULT 'waiting', in DB//
    private String type ; //walk_in, check_in//
	

    
    
    
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

	

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getReservationId() {
		return reservationId;
	}

	public void setReservationId(int reservationId) {
		this.reservationId = reservationId;
	}

	public Timestamp getEntryTimeToList() {
		return entryTimeToList;
	}

	public void setEntryTimeToList(Timestamp entryTimeToList) {
		this.entryTimeToList = entryTimeToList;
	}


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getNumberOfDiners() {
		return numberOfDiners;
	}

	public void setNumberOfDiners(int numberOfDiners) {
		this.numberOfDiners = numberOfDiners;
	}



	public Timestamp getExitTimeFromList() {
		return exitTimeFromList;
	}

	public void setExitTimeFromList(Timestamp exitTimeFromList) {
		this.exitTimeFromList = exitTimeFromList;
	}

	
	
	
	
}
