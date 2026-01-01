package data;


import java.util.ArrayList;

public class OpeningHours 
{
	private String day;//'SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'
	private ArrayList<TimeSlot> slots;
	
	/**
	 * 
	 * @param day the date of the opening hours
	 */
	public OpeningHours(String day) 
	{
		this.setDay(day);
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public ArrayList<TimeSlot> getSlots() {
		return slots;
	}

	public void setSlots(ArrayList<TimeSlot> slots) {
		this.slots = slots;
	}

}
