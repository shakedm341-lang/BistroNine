package data;

public class Bill 
{
	private int billId ;//incremente otomatically in the DB//
	private int reservationId ; 	//foreign key from Reservation table//
	private double totalAmount ;//
	private double totalAmountAfterDiscount;//
	private double discountSize;//
	private boolean isPaid  ;//DEFAULT FALSE//FALSE,true//
	private String discountType;//subscriber,customer //
	private String paymentMethod ;//Cash, Credit, App

	public Bill() 
	{
		
	}

	public int getBillId() {
		return billId;
	}

	public void setBillId(int billId) {
		this.billId = billId;
	}

	public int getReservationId() {
		return reservationId;
	}

	public void setReservationId(int reservationId) {
		this.reservationId = reservationId;
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public boolean isPaid() {
		return isPaid;
	}

	public void setPaid(boolean isPaid) {
		this.isPaid = isPaid;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public double getTotalAmountAfterDiscount() {
		return totalAmountAfterDiscount;
	}

	public void setTotalAmountAfterDiscount(double totalAmountAfterDiscount) {
		this.totalAmountAfterDiscount = totalAmountAfterDiscount;
	}

	public double getDiscountSize() {
		return discountSize;
	}

	public void setDiscountSize(double discountSize) {
		this.discountSize = discountSize;
	}

	public String getDiscountType() {
		return discountType;
	}

	public void setDiscountType(String discountType) {
		this.discountType = discountType;
	}
	
	
	

}
