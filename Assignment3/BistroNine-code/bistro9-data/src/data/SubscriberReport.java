package data;

import java.time.LocalDate;
import java.util.ArrayList;



public class SubscriberReport extends ReportManager
{
	//private int reportId;
	private ArrayList<Row> rows=new ArrayList<>();

	public SubscriberReport() 
	{

	}
	public void addRow(LocalDate date, int totalReservations, int totalWaiting) {
		this.rows.add(new Row(date, totalReservations, totalWaiting));
	}

	public ArrayList<Row> getRows() {
		return rows;
	}

	public void setRows(ArrayList<Row> rows) {
		this.rows = rows;
	}

	public static class Row  
	{
		private LocalDate reportDate;
		private int totalReservations; 
		private int totalWaiting;      

		public Row(LocalDate reportDate, int totalReservations, int totalWaiting) 
		{
			this.reportDate = reportDate;
			this.totalReservations = totalReservations;
			this.totalWaiting = totalWaiting;
		}

		public LocalDate getReportDate() {
			return reportDate;
		}



		public void setReportDate(LocalDate reportDate) {
			this.reportDate = reportDate;
		}



		public int getTotalReservations() {
			return totalReservations;
		}



		public void setTotalReservations(int totalReservations) {
			this.totalReservations = totalReservations;
		}



		public int getTotalWaiting() {
			return totalWaiting;
		}



		public void setTotalWaiting(int totalWaiting) {
			this.totalWaiting = totalWaiting;
		}
	}
}
