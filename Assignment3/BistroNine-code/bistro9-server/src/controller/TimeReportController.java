package controller;

import java.time.LocalDate;
import java.util.ArrayList;

import data.Bill;
import data.Message;
import data.TableReservation;
import data.TimeReport;

public class TimeReportController 
{
	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	
	public TimeReportController() 
	{

	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////managing messages //////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Handles messages received from the server and performs actions based on the
	 * command type.
	 *
	 * @param msg The message received from the server.
	 * @return An object representing the result of the action performed based on
	 *         the command type.
	 */
	public Object handleMessageFromServer(Message msg) 
	{

		switch (msg.command) // Checking the type of message sent from the server (what action should be
		// performed in the DB Controller)
		{
		case GET_TIME_REPORT_BY_RANGE_DATE:
			return getTimeReportByRangeDate(msg);
		default:
			System.out.println("Unknown task received.");
			return null;
		}
	}	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a time report based on a specified date range from the database.
	 * 
	 *
	 * @param msg The message containing the login details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 : LocalDate startDay(YYYY-MM-01), Location 1 : LocalDate endDay(YYYY-MM-28/29/30/31)]
	 * @return A TimeReport object containing the report data, or null if an error
	 *         occurs.
	 */
	private TimeReport getTimeReportByRangeDate(Message msg)
	{	
		@SuppressWarnings("unchecked") 
        ArrayList<Object> list = (ArrayList<Object>) msg.content; 

		TimeReport report = new TimeReport();
		
        // Set start Day in report object
        if (list.get(0) instanceof LocalDate) 
        {
        		report.setStartDay((LocalDate) list.get(0));
        } else {
            System.out.println("Error: Index 0 is not a LocalDate!");
            return null;
        }
		
		// Set end Day in report object
        if (list.get(1) instanceof LocalDate) 
        {
        		report.setEndDay((LocalDate) list.get(1));
        } else {
            System.out.println("Error: Index 1 is not a LocalDate!");
            return null;
        }
		
		if (!DBC.getTimeReportByRangeDateQuery(report)) // updating the report object with the data from the DB return false if an error occurred or true if success
		{
			System.out.println("Error retrieving time report from the database.");
			return null;
		} 
		else 
		{
			return report;
		}
        
        
        
        
	}
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////Automated tasks//////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Generates a monthly time report on the 1st day of each month. The report
	 * includes average arrival and average leaving times for each day of the previous
	 * month. The report is then added to the database.
	 * if the bistro was closed on a specific day, the average times for that day will be recorded as 0.
	 * if an error occurs while retrieving data for a specific day, the average times for that day will be recorded as -1.
	 */
	public static void  timeReportGenerate()
	{
		LocalDate today = LocalDate.now();

		//if the day is not the 1 in Month
		if (today.getDayOfMonth() != 1) {
			return; 
		}


		System.out.println("1st of the month detected. Starting report production...");

		TimeReport report = new TimeReport();

		//calculate first and last day of last month
		LocalDate lastMonth = today.minusMonths(1);//last month
		int daysInMonth = lastMonth.lengthOfMonth();//amount of days in last month (28/29/30/31)

		LocalDate startOfMonth = lastMonth.withDayOfMonth(1);//first day in the month (1)
		LocalDate endOfMonth = lastMonth.withDayOfMonth(daysInMonth); // last day in the month (28/29/30/31)


		report.setStartDay(startOfMonth);
		report.setEndDay(endOfMonth);

		//generatedAt DEFAULT CURRENT_TIMESTAMP in DB 

		report.setReportRange("monthly");
		report.setReportType("time");


		for (int i = 1; i <= daysInMonth; i++) 
		{

			LocalDate currentDate = lastMonth.withDayOfMonth(i);

			int avgArrival = DBC.getDailyAvgArrivalQuery(currentDate);//return the average arrival time(Customer arrival time - time of order) for the day else -1 if an error occurred if bistro close return 0

			if (avgArrival == -1) {
				System.out.println("Error retrieving average arrival time for date: " + currentDate);
				
			}

			int avgLeaving = DBC.getDailyAvgLeavingQuery(currentDate);//return the average leaving time(Customer leaving time - (Customer arrival time+  2 Hours)) for the day else -1 if an error occurred if bistro close return 0

			if (avgLeaving == -1) 
			{
				System.out.println("Error retrieving average leaving time for date: " + currentDate);
				
			}

			report.addRow(currentDate, avgArrival, avgLeaving);
		}

		if (!DBC.addTimeReportQuery(report)) //adding the report to the DB in report_manager and time_report return false if an error occurred or true if success
		{
			System.out.println("Error adding time report to the database.");
		} else {
			System.out.println("Time report successfully added to the database.");
		}
	}

}
