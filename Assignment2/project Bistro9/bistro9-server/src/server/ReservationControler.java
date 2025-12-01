package server;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;


//controler

public class ReservationControler 
{
	private DataBaseControler DBC=DataBaseController.getInstance();//התממשקות עם הדאטה בייס קונטרולר;
	
	public ReservationControler() 
	{
	
	}
	
	public Object handleMessageFromServer(message msg) 
	{
		
		switch (msg.command) //בדיקה איזה הודעה נשלחה מהשרת(איזה פעולה צריך לעשות)
		{
	    case GET_ALL_RESERVATIONS:
	    	return getAllReservations(msg);
	    	
	    case UPDATE_RESERVATION_DETAILS:
	    	return updateReservationDetails(msg);
	    	
	    default:
	        System.out.println("Unknown task received.");
	        return null;
		}
	}
		
	private ArrayList<TableReservation> getAllReservations(message msg)
	{
		
    	ArrayList<String> reservationsListAsStr = new ArrayList<>();
    	ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();
    	
    	reservationsListAsStr = DBC.getAllReservationsQuery();//יצירת שאילתה חדשה בדאטה בייס שמחזירה את כל ההזמנות הקיימות כרשימה של מחרוזות(כל מחרוזת זו שורה בטבלה)
    	
    	for (String resAsStr : reservationsListAsStr)//לולאה שמבצעת המרה של רשימת המחרוזות לרשימת אובייקטי הזמנת שולחן
    	{
    	    //פיצול המחרוזת לעמודות של הטבלה מהדאטה בייס בכדי לקחת את הנתונים שהועברו
    	    String[] dataRes = resAsStr.split(",");
    	    
    	    //יצירת אובייקט הזמנת שולחן עם הנתונים שהתקבלו מהדאטה בייס
    	    TableReservation resAsTableRes = new TableReservation();
    	    resAsTableRes.setReservationId(dataRes[0]);
    	    resAsTableRes.setReservationDate(dataRes[1]);
    	    resAsTableRes.setNumberOfDiners(dataRes[2]);
    	    resAsTableRes.setConfirmationCode(dataRes[3]);
    	    resAsTableRes.setSubscriberId(dataRes[4]);
    	    resAsTableRes.setDateOfMakeReservation(dataRes[5]);
    	    
    	    //הוספת האובייקט שיצרנו לרשימת ההזמנות מסוג הזמנת שולחן
    	    reservationsListAsTableRes.add(resAsTableRes);
    	}
    	
    	return reservationsListAsTableRes;//החזרה לשרת רשימת ההזמנות כרשימה של אובייקטי בזמנות שולחן 
	}
	
	private boolean updateReservationDetails(message msg)
	{
		ArrayList<String> list = msg.contant;//רשימה של מחרוזות שמכילה איזה הזמנה יש לעדכן ואת המידע שצריך לעדכן
    	

    	//יצירת אובייקט חדש של הזמנת שולחן עם ההזמנה שרוצים לעדכן ועם הפרטים שיש לעדכן
    	TableReservation res = new TableReservation();
	    res.setReservationId(list.get(0));
	    res.setReservationDate(list.get(1));
	    res.setNumberOfDiners(list.get(2));

	    //מעדכן את פרטי ההזמנה בדאטה בייס
	    //החזרה לשרת האם פעולת העדכון התבצע כראוי או לא
	    return DBC.updateReservationDetailsQuery(res); 
	    
	}
	
	/*public void  GetAllReservations() 
	{
		
	}
	
	public void updateReservation() 
	{
		
	}
	
	public void createReservation() 
	{
		
	}
	
	public void cencelReservation() 
	{
		
	}
	
	public void getReservation() 
	{
		
	}
	
	public void saveReservation() 
	{
		
	}
	
	public void getTimesData() 
	{
		
	}
	
	public void returnTimeOfMakingReservation() 
	{
		LocalDate todayDate = LocalDate.now().toString();
		
        LocalTime nowTime = LocalTime.now().toString(); 
        
        String dateString = todayDate.toString();
        String timeString = nowTime.toString();
        
        return dateString + "," + timeString;
	}
	
	public void viewReservation() 
	{
		
	}
	
	public void sendMessageToCustomer() 
	{
		
	}
	
	
	public void viewVisits() 
	{
		
	}
	
	public void returnStatusOfReservation() 
	{
		
	}
	
	public static void main(String[] args) 
	{
		
	}*/

}
