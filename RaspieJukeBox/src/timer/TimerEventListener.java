package timer;

public interface TimerEventListener {
	public void endTimeAchieved(TimerEvent ev);

	public void timerInterrupted(TimerEvent ev);
}
