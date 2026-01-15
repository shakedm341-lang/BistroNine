package gui;

import java.util.ArrayList;
import data.TableReservation;

/**
 * Interface for components that display visit history (TableReservation list).
 * Screens implementing this interface can be updated with a new list of visits.
 */
public interface IVisitHistory {
    /**
     * Updates the component with a list of table reservations (visits).
     * 
     * @param reservations The list of {@link TableReservation} objects to display.
     */
    void setReservationsList(ArrayList<TableReservation> reservations);
}

