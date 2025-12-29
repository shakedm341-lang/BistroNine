package gui;

import java.net.URL;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Subscriber;
import javafx.fxml.Initializable;

public class TabWaitingListController implements Initializable {

    private ClientController client;
    

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Nothing to init specifically for UI yet
    }

    // --- Must-have methods so LiveDashboardController won't crash ---

    public void initData(ClientController client) {
        this.client = client;
        
        System.out.println("TabWaitingList: Initialized (Placeholder mode)");
    }

    public void refreshData() {
        // כאן היינו אמורים לשלוח הודעה לשרת.
        // כרגע אנחנו לא עושים כלום כדי למנוע שגיאות.
        System.out.println("TabWaitingList: refreshData called - Not implemented on server yet.");
        
        /* בעתיד תוסיף כאן:
           if (client != null) {
               client.accept(new Message(MessageType.GET_WAITING_LIST, null));
           }
        */
    }
    
    // פונקציה לעדכון עתידי - יכולה להישאר ריקה כרגע
    public void updateTableData(Object data) {
        // Future implementation
    }
}