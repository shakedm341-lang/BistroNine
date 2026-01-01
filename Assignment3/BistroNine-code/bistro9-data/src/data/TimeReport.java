package data;

import java.time.LocalDate;
import java.util.ArrayList;

public class TimeReport extends ReportManager
{
	//private int reportId;
	private ArrayList<Row> rows=new ArrayList<>();


	public TimeReport() 
	{

	}

	public void addRow(LocalDate date, int avgArrival, int avgLeaving) {
		this.rows.add(new Row(date, avgArrival, avgLeaving));
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
		private int avgArrival;
		private int avgLeaving;

		public Row(LocalDate reportDate, int avgArrival, int avgLeaving) {
			this.reportDate = reportDate;
			this.avgArrival = avgArrival;
			this.avgLeaving = avgLeaving;
		}
		public LocalDate getReportDate() {
			return reportDate;
		}


		public void setReportDate(LocalDate reportDate) {
			this.reportDate = reportDate;
		}


		public int getAvgArrival() {
			return avgArrival;
		}


		public void setAvgArrival(int avgArrival) {
			this.avgArrival = avgArrival;
		}


		public int getAvgLeaving() {
			return avgLeaving;
		}


		public void setAvgLeaving(int avgLeaving) {
			this.avgLeaving = avgLeaving;
		}
	}

}
