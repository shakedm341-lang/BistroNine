package gui;

import java.io.Serializable;

// A stub class to represent a User in the GUI context for testing purposes
public class StubUser implements Serializable {
    
    private String username;
    private String password;
    private String userType; 

    //  (Constructor)
    public StubUser(String username, String password, String userType) {
        this.username = username;
        this.password = password;
        this.userType = userType;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getUserType() {
        return userType;
    }
    
    // Setters 
    public void setUserType(String userType) {
        this.userType = userType;
    }
}