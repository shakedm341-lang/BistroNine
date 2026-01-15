package data;

/**
 * Entity class representing a Bill in the restaurant system.
 * Stores financial details, payment status, and discount information associated with a reservation.
 */
public class Bill 
{
	/** Unique identifier for the bill. Auto-incremented in the database. */
	private int billId ;//incremente otomatically in the DB//
	
	/** The ID of the reservation associated with this bill (Foreign Key). */
	private int reservationId ; 	//foreign key from Reservation table//
	
	/** The initial total amount before any discounts are applied. */
	private double totalAmount ;//
	
	/** The final amount to be paid after applying discounts. */
	private double totalAmountAfterDiscount;//
	
	/** The value/amount of the discount applied. */
	private double discountSize;//
	
	/** Payment status. True if paid, False otherwise. Default is false. */
	private boolean isPaid  ;//DEFAULT FALSE//FALSE,true//
	
	/** The type of discount applied (e.g., "subscriber", "customer"). */
	private String discountType;//subscriber,customer //
	
	/** The method used for payment (e.g., "Cash", "Credit", "App"). */
	private String paymentMethod ;//Cash, Credit, App
	
	/**
	 * Default constructor.
	 */
	public Bill() 
	{
		
	}
	
	/**
	 * Gets the unique bill identifier.
	 * @return The bill ID.
	 */
	public int getBillId() {
		return billId;
	}

	/**
	 * Sets the bill identifier.
	 * @param billId The unique ID to set.
	 */
	public void setBillId(int billId) {
		this.billId = billId;
	}

	/**
	 * Gets the associated reservation ID.
	 * @return The reservation ID.
	 */
	public int getReservationId() {
		return reservationId;
	}

	/**
	 * Sets the associated reservation ID.
	 * @param reservationId The reservation ID to link.
	 */
	public void setReservationId(int reservationId) {
		this.reservationId = reservationId;
	}

	/**
	 * Gets the total amount before discount.
	 * @return The total amount.
	 */
	public double getTotalAmount() {
		return totalAmount;
	}

	/**
	 * Sets the total amount before discount.
	 * @param totalAmount The amount to set.
	 */
	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}

	/**
	 * Checks if the bill has been paid.
	 * @return True if paid, false otherwise.
	 */
	public boolean isPaid() {
		return isPaid;
	}

	/**
	 * Sets the payment status of the bill.
	 * @param isPaid True to mark as paid, false for unpaid.
	 */
	public void setPaid(boolean isPaid) {
		this.isPaid = isPaid;
	}

	/**
	 * Gets the payment method used.
	 * @return The payment method as a String.
	 */
	public String getPaymentMethod() {
		return paymentMethod;
	}

	/**
	 * Sets the payment method.
	 * @param paymentMethod The method (e.g., "Cash", "Credit") to set.
	 */
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	/**
	 * Gets the total amount after the discount calculation.
	 * @return The final payable amount.
	 */
	public double getTotalAmountAfterDiscount() {
		return totalAmountAfterDiscount;
	}

	/**
	 * Sets the total amount after discount.
	 * @param totalAmountAfterDiscount The final amount to set.
	 */
	public void setTotalAmountAfterDiscount(double totalAmountAfterDiscount) {
		this.totalAmountAfterDiscount = totalAmountAfterDiscount;
	}

	/**
	 * Gets the size/value of the discount.
	 * @return The discount size.
	 */
	public double getDiscountSize() {
		return discountSize;
	}

	/**
	 * Sets the size/value of the discount.
	 * @param discountSize The discount value to set.
	 */
	public void setDiscountSize(double discountSize) {
		this.discountSize = discountSize;
	}

	/**
	 * Gets the type of discount applied.
	 * @return The discount type (e.g., "subscriber").
	 */
	public String getDiscountType() {
		return discountType;
	}

	/**
	 * Sets the type of discount.
	 * @param discountType The type of discount to set.
	 */
	public void setDiscountType(String discountType) {
		this.discountType = discountType;
	}

}