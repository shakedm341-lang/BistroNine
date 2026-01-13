package data;

import java.time.LocalTime;

/**
 * Entity class representing a specific time interval.
 * Used to define opening and closing boundaries for restaurant operations or table availability.
 */
public class TimeSlot 
{
	
        public LocalTime open;
        public LocalTime close;
        
		/**
		 * Constructor for TimeSlot.
		 * * @param open  The starting time of the slot.
		 * @param close The ending time of the slot.
		 */
        public TimeSlot(LocalTime open, LocalTime close) 
        { 
	        	this.open = open;
	        	this.close = close; 
        }
        
        
		/**
		 * Gets the opening time of this slot.
		 * @return The start time as LocalTime.
		 */
		public LocalTime getOpen() 
		{
			return open;
        }

		/**
		 * Sets the opening time of this slot.
		 * @param open The start time to set.
		 */
		public void setOpen(LocalTime open)
		{
			this.open = open;
		}

		/**
		 * Gets the closing time of this slot.
		 * @return The end time as LocalTime.
		 */
		public LocalTime getClose() {
			return close;
		}

		/**
		 * Sets the closing time of this slot.
		 * @param close The end time to set.
		 */
		public void setClose(LocalTime close) {
			this.close = close;
		}
}