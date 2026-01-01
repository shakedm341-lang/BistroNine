package data;

import java.sql.Timestamp;
import java.time.LocalDate;

public class ReportManager 
{
	private int reportId ;//  AUTO_INCREMENT in DB 
	private LocalDate startDay ;
	private LocalDate endDay ;
	private Timestamp generatedAt ;//DEFAULT CURRENT_TIMESTAMP,
	private String reportRange ;//ENUM('monthly', 'weekly','daily')
	private String reportType;// ENUM('time', 'subscriber')

	
	
	
	public ReportManager() 
	{
		
	}

	public int getReportId() {
		return reportId;
	}

	public void setReportId(int reportId) {
		this.reportId = reportId;
	}

	public LocalDate getStartDay() {
		return startDay;
	}

	public void setStartDay(LocalDate startDay) {
		this.startDay = startDay;
	}

	public LocalDate getEndDay() {
		return endDay;
	}

	public void setEndDay(LocalDate endDay) {
		this.endDay = endDay;
	}

	public Timestamp getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(Timestamp generatedAt) {
		this.generatedAt = generatedAt;
	}

	public String getReportRange() {
		return reportRange;
	}

	public void setReportRange(String reportRange) {
		this.reportRange = reportRange;
	}

	public String getReportType() {
		return reportType;
	}

	public void setReportType(String reportType) {
		this.reportType = reportType;
	}

}
