package data;

import java.io.Serializable;

public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public TypeMessage type;
	public Object content;
	public Command command;
}
