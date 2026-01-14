package gui;

import java.util.ArrayList;
import data.HistoryReservation;

public interface IReservationViewer {
    /**
     * Method for screens using HistoryReservation (e.g., ReservationManagementController, MyReservationsController)
     */
    void setReservationsList(ArrayList<HistoryReservation> reservations);
}
