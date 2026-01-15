CREATE TABLE customer (
    customerId INT AUTO_INCREMENT,
    phoneNumber VARCHAR(100) UNIQUE,
    email VARCHAR(100) UNIQUE,
    CONSTRAINT check_contact_info CHECK (phoneNumber IS NOT NULL OR email IS NOT NULL),
    PRIMARY KEY (customerId)
);

CREATE TABLE subscriber (
    subscriberId INT AUTO_INCREMENT,
    customerId INT NOT NULL UNIQUE,
    firstName VARCHAR(100) NOT NULL,
    lastName VARCHAR(100) NOT NULL,
    type ENUM('subscriber', 'restaurant representative', 'restaurant manager') NOT NULL,
    personalInfo VARCHAR(1000),
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    PRIMARY KEY (subscriberId),
    FOREIGN KEY (customerId) REFERENCES customer(customerId) ON UPDATE CASCADE
);

CREATE TABLE restaurant_tables (
    tableId INT AUTO_INCREMENT,
    seatsNumber INT NOT NULL,
    location ENUM('inside', 'bar', 'outside') NOT NULL,
    status ENUM('available', 'occupied', 'cancelled') NOT NULL,
    PRIMARY KEY (tableId)
);

CREATE TABLE table_reservations (
    reservationId INT AUTO_INCREMENT,
    tableId INT,
    numberOfDiners INT NOT NULL,
    confirmationCode INT NOT NULL UNIQUE,
    customerId INT NOT NULL,
    reservationDate DATETIME NOT NULL,
    dateOfMakeReservation DATETIME DEFAULT CURRENT_TIMESTAMP,
    arrivalTime DATETIME,
    leavingTime DATETIME,
    status ENUM('active','arrived' ,'cancelled', 'completed','waiting' ) NOT NULL DEFAULT 'active',
    PRIMARY KEY (reservationId),
    FOREIGN KEY (customerId) REFERENCES customer(customerId) ON UPDATE CASCADE,
    FOREIGN KEY (tableId) REFERENCES restaurant_tables(tableId)
);

CREATE TABLE weekly_hours (
    dayOfWeek ENUM('SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY') NOT NULL,
    openingTime TIME NOT NULL,
    closingTime TIME NOT NULL,
    PRIMARY KEY (dayOfWeek, openingTime)
);

CREATE TABLE special_hours (
    specificDate DATE NOT NULL,
    openingTime TIME NOT NULL,
    closingTime TIME NOT NULL,
    PRIMARY KEY (specificDate, openingTime)
);

CREATE TABLE restaurant_discount (
    type_customer ENUM('subscriber', 'customer'),
    discount DECIMAL(5,2) DEFAULT 0.00,
    PRIMARY KEY (type_customer)
);

CREATE TABLE bills (
    billId INT AUTO_INCREMENT,
    reservationId INT NOT NULL,
    totalAmount DECIMAL(10, 2) DEFAULT 0.00,
    totalAmountAfterDiscount DECIMAL(10, 2) DEFAULT 0.00,
    discountPercentage DECIMAL(5,2) DEFAULT 0.00,
    isPaid BOOLEAN DEFAULT FALSE,
    discountType ENUM('subscriber', 'customer') DEFAULT NULL,
    paymentMethod ENUM('cash', 'credit', 'app') DEFAULT NULL,
    PRIMARY KEY (billId),
    FOREIGN KEY (reservationId) REFERENCES table_reservations(reservationId)
);

CREATE TABLE waiting_list (
    waitingId INT NOT NULL AUTO_INCREMENT,
    reservationId INT NOT NULL,
    numberOfDiners INT NOT NULL,
    entryTimeToList TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    exitTimeFromList TIMESTAMP,
    status ENUM('waiting', 'seated', 'cancelled','notified') DEFAULT 'waiting',
    type ENUM('walk_in', 'check_in') NOT NULL,
    PRIMARY KEY (waitingId),
    FOREIGN KEY (reservationId) REFERENCES table_reservations(reservationId)
);

CREATE TABLE report_manager (
    reportId INT AUTO_INCREMENT,
    startDay DATE NOT NULL,
    endDay DATE NOT NULL,
    generatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reportRange ENUM('monthly', 'weekly','daily') NOT NULL,
    reportType ENUM('time', 'subscriber') NOT NULL,
    PRIMARY KEY (reportId),
    UNIQUE KEY unique_report_range (startDay, endDay, reportType)
);

CREATE TABLE time_report (
    reportId INT NOT NULL,
    reportDate DATE NOT NULL,
    avgArrival INT NOT NULL,
    avgLeaving INT NOT NULL,
    PRIMARY KEY (reportId, reportDate),
    FOREIGN KEY (reportId) REFERENCES report_manager(reportId) ON DELETE CASCADE
);

CREATE TABLE subscriber_report (
    reportId INT NOT NULL,
    reportDate DATE NOT NULL,
    totalReservations INT NOT NULL,
    totalWaiting INT NOT NULL,
    PRIMARY KEY (reportId, reportDate),
    FOREIGN KEY (reportId) REFERENCES report_manager(reportId) ON DELETE CASCADE
);