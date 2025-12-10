package gui;

import javafx.beans.property.SimpleStringProperty;

public class ConnectedClient {
    private final SimpleStringProperty clientIp;
    private final SimpleStringProperty hostName;
    private final SimpleStringProperty status;

    public ConnectedClient(String ip, String host, String status) {
        this.clientIp = new SimpleStringProperty(ip);
        this.hostName = new SimpleStringProperty(host);
        this.status = new SimpleStringProperty(status);
    }

    public String getClientIp() { return clientIp.get(); }
    public void setClientIp(String ip) { this.clientIp.set(ip); }

    public String getHostName() { return hostName.get(); }
    public void setHostName(String host) { this.hostName.set(host); }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
}