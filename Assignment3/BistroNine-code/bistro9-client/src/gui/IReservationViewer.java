package gui;

import java.util.ArrayList;
import data.HistoryReservation;

/**
 * Interface for components that display a list of reservation history.
 * Screens implementing this interface can be updated with a new list of reservations.
 */
public interface IReservationViewer {
    /**
     * Updates the component with a list of historical reservations.
     * This method is typically used by controllers to populate reservation views
     * (e.g., ReservationManagementController, MyReservationsController).
     * 
     * @param reservations The list of {@link HistoryReservation} objects to display.
     */
    void setReservationsList(ArrayList<HistoryReservation> reservations);
}
