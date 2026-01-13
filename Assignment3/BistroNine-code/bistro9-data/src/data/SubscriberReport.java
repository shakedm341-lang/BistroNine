package data;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Entity class representing a report for subscriber activity.
 * This report includes daily statistics on the number of reservations and customers in the waiting list.
 */
public class SubscriberReport extends ReportManager
{
	private ArrayList<Row> rows=new ArrayList<>();

	/**
	 * Default constructor for SubscriberReport.
	 */
	public SubscriberReport() 
	{

	}
	
	/**
	 * Adds a new data row to the report.
	 * * @param date The date for which the statistics are recorded.
	 * @param totalReservations The total count of reservations for that day.
	 * @param totalWaiting The total count of people who were on the waiting list.
	 */
	public void addRow(LocalDate date, int totalReservations, int totalWaiting) {
		this.rows.add(new Row(date, totalReservations, totalWaiting));
	}

	/**
	 * Gets all the rows (data entries) contained in this report.
	 * @return An ArrayList of Row objects.
	 */
	public ArrayList<Row> getRows() {
		return rows;
	}

	/**
	 * Sets the list of rows for this report.
	 * @param rows An ArrayList of Row objects to set.
	 */
	public void setRows(ArrayList<Row> rows) {
		this.rows = rows;
	}

	/**
	 * Static inner class representing a single row of data in the Subscriber Report.
	 */
	public static class Row  
	{
		private LocalDate reportDate;
		private int totalReservations; 
		private int totalWaiting;      

		/**
		 * Constructor for Row.
		 * * @param reportDate The date of the record.
		 * @param totalReservations Total reservations on this date.
		 * @param totalWaiting Total people waiting on this date.
		 */
		public Row(LocalDate reportDate, int totalReservations, int totalWaiting) 
		{
			this.reportDate = reportDate;
			this.totalReservations = totalReservations;
			this.totalWaiting = totalWaiting;
		}

		/**
		 * Gets the date for this specific row.
		 * @return The report date.
		 */
		public LocalDate getReportDate() {
			return reportDate;
		}

		/**
		 * Sets the date for this row.
		 * @param reportDate The date to set.
		 */
		public void setReportDate(LocalDate reportDate) {
			this.reportDate = reportDate;
		}

		/**
		 * Gets the total number of reservations recorded in this row.
		 * @return Total reservations.
		 */
		public int getTotalReservations() {
			return totalReservations;
		}

		/**
		 * Sets the total number of reservations for this row.
		 * @param totalReservations The number to set.
		 */
		public void setTotalReservations(int totalReservations) {
			this.totalReservations = totalReservations;
		}

		/**
		 * Gets the total number of people on the waiting list for this row.
		 * @return Total waiting count.
		 */
		public int getTotalWaiting() {
			return totalWaiting;
		}

		/**
		 * Sets the total number of people on the waiting list for this row.
		 * @param totalWaiting The count to set.
		 */
		public void setTotalWaiting(int totalWaiting) {
			this.totalWaiting = totalWaiting;
		}
	}
}