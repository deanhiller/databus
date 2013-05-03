package controllers.api;

public class StateDocument {

	private String user;
	private String description;
	private String state;
	
	public StateDocument(String user, String description, String state) {
		super();
		this.user = user;
		this.description = description;
		this.state = state;
	}
	
	public String getUser() {
		return user;
	}
	public String getDescription() {
		return description;
	}
	public String getState() {
		return state;
	}
	
}
