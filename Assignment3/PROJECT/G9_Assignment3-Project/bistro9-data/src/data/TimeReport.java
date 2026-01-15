package data;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Entity class representing a report focused on timing statistics.
 * This report tracks average arrival and leaving times per day to analyze restaurant occupancy.
 */
public class TimeReport extends ReportManager
{
	private ArrayList<Row> rows=new ArrayList<>();

	/**
	 * Default constructor for TimeReport.
	 */
	public TimeReport() 
	{

	}

	/**
	 * Adds a new data row to the timing report.
	 * * @param date The date for which the statistics are calculated.
	 * @param avgArrival The average arrival time (typically in minutes from start of day).
	 * @param avgLeaving The average leaving time (typically in minutes from start of day).
	 */
	public void addRow(LocalDate date, int avgArrival, int avgLeaving) {
		this.rows.add(new Row(date, avgArrival, avgLeaving));
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
	 * Static inner class representing a single row of data in the Time Report.
	 */
	public static class Row 
	{
		private LocalDate reportDate;
		private int avgArrival;
		private int avgLeaving;

		/**
		 * Constructor for Row.
		 * * @param reportDate The date of the record.
		 * @param avgArrival The calculated average arrival time.
		 * @param avgLeaving The calculated average leaving time.
		 */
		public Row(LocalDate reportDate, int avgArrival, int avgLeaving) {
			this.reportDate = reportDate;
			this.avgArrival = avgArrival;
			this.avgLeaving = avgLeaving;
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
		 * Gets the average arrival time recorded in this row.
		 * @return Average arrival time.
		 */
		public int getAvgArrival() {
			return avgArrival;
		}

		/**
		 * Sets the average arrival time for this row.
		 * @param avgArrival The time to set.
		 */
		public void setAvgArrival(int avgArrival) {
			this.avgArrival = avgArrival;
		}

		/**
		 * Gets the average leaving time recorded in this row.
		 * @return Average leaving time.
		 */
		public int getAvgLeaving() {
			return avgLeaving;
		}

		/**
		 * Sets the average leaving time for this row.
		 * @param avgLeaving The time to set.
		 */
		public void setAvgLeaving(int avgLeaving) {
			this.avgLeaving = avgLeaving;
		}
	}

}