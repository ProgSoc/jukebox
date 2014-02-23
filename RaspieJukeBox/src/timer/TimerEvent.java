package timer;

public class TimerEvent {
	private TimerTask task;

	public TimerEvent(TimerTask task) {
		this.task = task;
	}

	public TimerTask getTask() {
		return task;
	}
}
