package gui;

import java.util.ArrayList;
import data.TableReservation;

public interface IReservationViewer {
    /**
     * מתודה שכל מסך חייב לממש כדי לקבל את רשימת ההזמנות
     */
    void setReservationsList(ArrayList<TableReservation> reservations);
}