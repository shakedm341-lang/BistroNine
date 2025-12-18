package data;

import java.time.LocalTime;

public class TimeSlot 
{
	
        public LocalTime open;
        public LocalTime close;
        
		/**
		 * 
		 * @param open  opening time
		 * @param close closing time
		 */
        public TimeSlot(LocalTime open, LocalTime close) 
        { 
	        	this.open = open;
	        	this.close = close; 
        	}
        
        
        
		public LocalTime getOpen() 
		{
			return open;
        }
		public void setOpen(LocalTime open)
		{
			this.open = open;
		}

		public LocalTime getClose() {
			return close;
		}

		public void setClose(LocalTime close) {
			this.close = close;
		}
}
