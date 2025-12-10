package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import data.TableReservation;

public class DataBaseController {

    private static DataBaseController instance;
    
    // DB connection settings data
    private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant_db?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
    private static String dbPassword; 
    private static final String USER = "root";

    // settings data for the "connection pool" 
    private static final int MAX_POOL_SIZE = 10;//Max num of connections to the DB in the "connection pool"
    private static final long MAX_IDLE_TIME = 5000;//Max time a connection can "sit in the connection pool" without being used
    private static final long CHECK_INTERVAL = 2;//How often does the "pool cleaner" run and delete unused connections in the "connection pool"?

    private BlockingQueue<PooledConnection> connectionPool;//The "connection pool"
    private ScheduledExecutorService cleanerService;//The "Pool Cleaner"

	/**
	 * Private constructor to prevent instantiation from outside. Initializes the
	 * connection pool and starts the cleanup timer.
	 */
    private DataBaseController() 
    {
        connectionPool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        startCleanupTimer();
        System.out.println("DataBaseController & Connection Pool Initialized.");
    }

	/**
	 * * Initializes the DataBaseController singleton with the provided password.
	 * This method must be called before getInstance().
	 *
	 * @param password The password for the database connection.
	 */
    public static void initiateDBC(String password) {
        dbPassword = password;
        if (instance == null) {
            instance = new DataBaseController();
        }
    }
    
    /**
	 * Retrieves the singleton instance of DataBaseController.
	 *@return The singleton instance of DataBaseController.
	 */
    public static DataBaseController getInstance() 
    {
        return instance;
    }

    
	/*////////////////////////////////////////////////////////////////////////////////////////////////////////////
    Managing the "connection pool"
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////*/
    
    
    /**
	 * Returns a connection from the pool or creates a new one if the pool is empty.
	 *
	 * @return A PooledConnection object.
	 */
    private PooledConnection getConnection() 
    {
        PooledConnection pConn = connectionPool.poll();//Try to get a connection from the pool
        
        if (pConn == null) {
            return createNewPhysicalConnection();//If there are no available connections in the pool, create a new one
        }
        
        pConn.touch();//Update the last used time
        return pConn;
    }
    
    /**
	 * * Releases a connection back to the pool or closes it if the pool is full.
	 *
	 * @param pConn The PooledConnection to be released back to the pool.
	 * @return void
	 */
    private void releaseConnection(PooledConnection pConn) 
    {
        if (pConn != null) {
            pConn.touch();//Update the last used time
            boolean added = connectionPool.offer(pConn);//Try to add the connection back to the pool
            
            if (!added) {
                pConn.closePhysicalConnection();
            }
        }
    }

    /**
  	 * * Creates a new physical database connection wrapped in a PooledConnection.
  	 *
  	 * 
  	 * @return A new PooledConnection object.
  	 */
    private PooledConnection createNewPhysicalConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");//Load the MySQL JDBC driver
            Connection conn = DriverManager.getConnection(DB_URL, USER, dbPassword);//Create a new connection
            
            return new PooledConnection(conn);//Wrap the connection in a PooledConnection and return it
        } catch (Exception e) {
            System.out.println("Error creating new connection: " + e.getMessage());
            return null;
        }
    }

    /**
	 * * Starts a timer that periodically checks for idle connections in the pool and closes them if they have been idle for too long.
	 *  @return void
	 */
    private void startCleanupTimer() 
    {
        cleanerService = Executors.newSingleThreadScheduledExecutor();//Create a single-threaded scheduled executor
        cleanerService.scheduleAtFixedRate(() -> 
        {
            if (connectionPool.isEmpty()) return;
            
            List<PooledConnection> activeConnections = new ArrayList<>();//Temporary list to hold active connections
            connectionPool.drainTo(activeConnections);//Remove all connections from the pool
            
            long now = System.currentTimeMillis();
            for (PooledConnection pConn : activeConnections)//Loop through all connections 
            {//Check if the connection has been idle for too long
                
                if (now - pConn.getLastUsed() > MAX_IDLE_TIME) {
                    pConn.closePhysicalConnection();
                } else {
                    connectionPool.offer(pConn);
                }
            }
        }, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.SECONDS);
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////////////////////
    The system's queries in the DB
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////*/

    /**
	 * * Retrieves all table reservations from the database.
	 *  @return A list of strings representing the reservations.
	 */
    public ArrayList<ArrayList<Object>> getAllReservationsQuery() 
    {
    	
    	ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();
        
        PooledConnection pConn = null;//The connection from the "connection pool"
        PreparedStatement ps = null;//The SQL statement to be executed
        ResultSet rs = null;//The result set of the executed query

        try {
            pConn = getConnection();//Get a connection from the "connection pool"
            if (pConn == null) 
            {
            	return null;
            }

            String query = "SELECT * FROM tablereservations";//The SQL query to be executed
            
            ps = pConn.getConnection().prepareStatement(query);
            rs = ps.executeQuery();
            
            
            while (rs.next()) 
            {//Loop through the result set and build the reservation strings
            	ArrayList<Object> reservations = new ArrayList<>();
            	reservations.add(rs.getInt("reservationID"));
            	reservations.add(rs.getTimestamp("reservationDate"));
            	reservations.add(rs.getInt("numberOfDiners"));
            	reservations.add(rs.getInt("confirmationCode"));
            	reservations.add(rs.getInt("subscriberId"));
            	reservations.add(rs.getTimestamp("dateOfMakeReservation"));
            
            	allReservations.add(reservations);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            releaseConnection(pConn); 
        }
        return allReservations;
    }

    public boolean updateReservationDetailsQuery(TableReservation t) {
        PooledConnection pConn = null;//The connection from the "connection pool"
        PreparedStatement ps = null;//The SQL statement to be executed

        try {
            pConn = getConnection();//Get a connection from the "connection pool"
            if (pConn == null) return false;
            
            //SQL query to update the reservation details
            String sql = "UPDATE tablereservations SET ReservationDate = ?, numberOfDiners = ? WHERE reservationID = ?";
            
          //Set the parameters for the SQL query
            ps = pConn.getConnection().prepareStatement(sql);
            ps.setTimestamp(1, t.getReservationDate());
            ps.setInt(2, t.getNumberOfDiners());
            ps.setInt(3, t.getReservationId());
            
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            releaseConnection(pConn);
        }
    }
}