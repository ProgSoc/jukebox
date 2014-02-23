package raspiejukebox;

public class JukeBoxEvent {
	enum Type {
		QUEUE,
		STATE
	}
	
	private JukeBox.State state;
	public final Type type;
	
	public JukeBoxEvent(JukeBox jb, Type eventType) {
		state = jb.getState();
		type = eventType;
	}

	public JukeBox.State getState() {
		return state;
	}
}
