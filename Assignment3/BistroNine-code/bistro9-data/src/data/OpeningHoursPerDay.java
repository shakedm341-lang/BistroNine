package data;

import java.time.LocalDate;
import java.util.ArrayList;

public class OpeningHoursPerDay 
{
	private LocalDate day;
	private ArrayList<TimeSlot> slots;
	
	/**
	 * 
	 * @param day the date of the opening hours
	 */
	public OpeningHoursPerDay(LocalDate day) 
	{
		this.setDay(day);
	}

	
    public ArrayList<TimeSlot> getSlots() {
		return slots;
	}

	public void setSlots(ArrayList<TimeSlot> slots) {
		this.slots = slots;
	}

	public LocalDate getDay() {
		return day;
	}

	public void setDay(LocalDate day) {
		this.day = day;
	}

	

}
