package server;
//antity

public class TableReservation 
{
	private int reservationId;
	private String  ReservationDate;
	private int numberOfDiners;
	private int confirmationCode;
	private int subscriberId;
	private String DateOfMakeReservation;
	
	public TableReservation() 
	{
		
	}
	

	public int getReservationId() 
	{
		return reservationId;
	}
	public void setReservationId(String reservationId) 
	{
		this.reservationId = Integer.parseInt(reservationId);
	}


	public String getReservationDate() 
	{
		return ReservationDate;
	}
	public void setReservationDate(String reservationDate) 
	{
		ReservationDate = reservationDate;
	}


	public int getNumberOfDiners() 
	{
		return numberOfDiners;
	}
	public void setNumberOfDiners(String numberOfDiners) 
	{
		this.numberOfDiners = Integer.parseInt(numberOfDiners);
	}


	public int getConfirmationCode() 
	{
		return confirmationCode;
	}
	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = Integer.parseInt(confirmationCode);
	}


	public int getSubscriberId() 
	{
		return subscriberId;
	}
	public void setSubscriberId(String subscriberId) 
	{
		this.subscriberId = Integer.parseInt(subscriberId);
	}


	public String getDateOfMakeReservation() 
	{
		return DateOfMakeReservation;
	}
	public void setDateOfMakeReservation(String dateOfMakeReservation) 
	{
		DateOfMakeReservation = dateOfMakeReservation;
	}
}
