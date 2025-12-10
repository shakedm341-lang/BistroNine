package controller;

import java.sql.Connection;
import java.sql.SQLException;

public class PooledConnection 
{
    private Connection connection;//The actual DB connection
    private long lastUsed;//The last time this connection was used 

	/**
	 * Constructor to create a PooledConnection with the given Connection.
	 *
	 * @param connection The actual database connection.
	 */
    public PooledConnection(Connection connection) {
        this.connection = connection;
        this.lastUsed = System.currentTimeMillis();
    }

	/**
	 * Getter for the actual database connection.
	 *
	 * @return The actual database connection.
	 */
    public Connection getConnection() {
        return connection;
    }

    // הוספנו Getter כדי שהטיימר יוכל לבדוק מתי השתמשו בחיבור לאחרונה
    public long getLastUsed() {
        return lastUsed;
    }

    public void touch() {
        this.lastUsed = System.currentTimeMillis();
    }
    
    public void closePhysicalConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}