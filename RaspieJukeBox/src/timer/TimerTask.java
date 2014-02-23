package timer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TimerTask implements Comparable<TimerTask> {
	protected AtomicLong endTimeMillis;
	protected List<TimerEventListener> listeners;

	public TimerTask(long endTimeMillis, TimerEventListener... listeners) {
		this.listeners = new LinkedList<TimerEventListener>();
		this.endTimeMillis = new AtomicLong(endTimeMillis);
		for (TimerEventListener l : listeners)
			this.listeners.add(l);
	}

	public void addTimerEventListener(TimerEventListener tel) {
		listeners.add(tel);
	}

	public void activate() {
		for (TimerEventListener l : listeners)
			l.endTimeAchieved(new TimerEvent(this));
	}

	public void interrupt() {
		for (TimerEventListener l : listeners)
			l.timerInterrupted(new TimerEvent(this));
	}

	public void setEndTime(long endTimeMillis) {
		this.endTimeMillis.set(endTimeMillis);
	}

	public long getEndTime() {
		return endTimeMillis.get();
	}

	public int compareTo(TimerTask tt) {
		return (int) Math.signum(tt.getEndTime() - getEndTime());
	}

	public String toString() {
		return "TimerTask{" + endTimeMillis.get() + ", " + listeners.toString()
				+ "}";
	}
}