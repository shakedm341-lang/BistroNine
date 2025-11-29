package common;

import java.io.Serializable;
import java.util.List;

/**
 * This class represents the data package exchanged between the client and the server.
 * It implements the Serializable interface to ensure the object can be converted into a byte stream for network transmission.
 * This object contains the message category, the specific command operation, and the data payload.
 */
public class Message implements Serializable {
	
	/**
     * The category of the message as defined in the TypeMessage enum.
     */
	private TypeMessage type;
    
	/**
     * The specific operation identifier or instruction for the receiver.
     */
	private String command;
    
	/**
     * The list of data objects containing the payload to be processed.
     */
	private List<Object> content;
	
	/**
     * Constructs a new Message instance with the specified category, command identifier, and data content.
     * * @param type The category of the message regarding the system domain.
     * @param command The string identifier describing the requested operation.
     * @param content The list containing the data objects to be transferred.
     */
	public Message(TypeMessage type, String command, List<Object> content) {
        this.type = type;
        this.command = command;
        this.content = content;
    }
    
	/**
     * Retrieves the category of this message.
     * * @return The TypeMessage enum value representing the message domain.
     */
	public TypeMessage gettypeMessage() {
    	return type;
    }
    
	/**
     * Retrieves the command identifier associated with this message.
     * * @return The string representing the specific operation command.
     */
	public String getcommand() {
    	return command;
    }
    
	/**
     * Retrieves the data payload of this message.
     * * @return The list of objects containing the transferred data.
     */
    public List<Object> getcontent() {
    	return content;
    }
    
}
