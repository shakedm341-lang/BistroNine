package data;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Entity class representing report configurations and metadata generated for the restaurant management.
 * Tracks report periods, generation timing, and categories.
 */
public class ReportManager 
{
	private int reportId ;//  AUTO_INCREMENT in DB 
	private LocalDate startDay ;
	private LocalDate endDay ;
	private Timestamp generatedAt ;//DEFAULT CURRENT_TIMESTAMP,
	private String reportRange ;//ENUM('monthly', 'weekly','daily')
	private String reportType;// ENUM('time', 'subscriber')

	/**
	 * Default constructor for ReportManager.
	 */
	public ReportManager() 
	{
		
	}

	/**
	 * Gets the unique report identifier.
	 * @return The report ID.
	 */
	public int getReportId() {
		return reportId;
	}

	/**
	 * Sets the unique report identifier.
	 * @param reportId The ID to set.
	 */
	public void setReportId(int reportId) {
		this.reportId = reportId;
	}

	/**
	 * Gets the start date of the reporting period.
	 * @return The start date as LocalDate.
	 */
	public LocalDate getStartDay() {
		return startDay;
	}

	/**
	 * Sets the start date of the reporting period.
	 * @param startDay The start date to set.
	 */
	public void setStartDay(LocalDate startDay) {
		this.startDay = startDay;
	}

	/**
	 * Gets the end date of the reporting period.
	 * @return The end date as LocalDate.
	 */
	public LocalDate getEndDay() {
		return endDay;
	}

	/**
	 * Sets the end date of the reporting period.
	 * @param endDay The end date to set.
	 */
	public void setEndDay(LocalDate endDay) {
		this.endDay = endDay;
	}

	/**
	 * Gets the timestamp indicating when the report was generated.
	 * @return The generation timestamp.
	 */
	public Timestamp getGeneratedAt() {
		return generatedAt;
	}

	/**
	 * Sets the timestamp indicating when the report was generated.
	 * @param generatedAt The timestamp to set.
	 */
	public void setGeneratedAt(Timestamp generatedAt) {
		this.generatedAt = generatedAt;
	}

	/**
	 * Gets the frequency range of the report (e.g., 'monthly', 'weekly', 'daily').
	 * @return The report range.
	 */
	public String getReportRange() {
		return reportRange;
	}

	/**
	 * Sets the frequency range of the report.
	 * @param reportRange The range to set.
	 */
	public void setReportRange(String reportRange) {
		this.reportRange = reportRange;
	}

	/**
	 * Gets the category or type of the report (e.g., 'time', 'subscriber').
	 * @return The report type.
	 */
	public String getReportType() {
		return reportType;
	}

	/**
	 * Sets the category or type of the report.
	 * @param reportType The type to set.
	 */
	public void setReportType(String reportType) {
		this.reportType = reportType;
	}

}