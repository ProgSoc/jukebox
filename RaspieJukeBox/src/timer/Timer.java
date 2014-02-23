package timer;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A timer. Takes a series of tasks and executes them when their end time is
 * met. When it executes them they are removed from the stack. I timedevent may
 * have its timer reset with no penalty.
 * 
 * @author Jableader
 */
public class Timer {
	public enum State {
		RUNNING, STOPPED
	}

	private State state;
	private LinkedList<TimerTask> tasks;
	private TimerThread tthread;

	public Timer(TimerTask... tasks) {
		this.tasks = new LinkedList<TimerTask>();
		this.state = State.STOPPED;
		this.tthread = new TimerThread();
		for (TimerTask t : tasks)
			this.tasks.add(t);
		tthread.start();
	}

	public void addTask(TimerTask t) {
		synchronized (tasks) {
			tasks.add(t);
		}

		if (tthread.getState() == Thread.State.TERMINATED) {
			tthread = new TimerThread();
			tthread.start();
		} else if (tthread.getState() == Thread.State.TIMED_WAITING)
			tthread.interrupt();

		state = Timer.State.RUNNING;
	}

	/** Abruptly halt the timer */
	public void halt() {
		this.state = State.STOPPED;
		tthread.interrupt();
	}

	public void forceCheck() {
		switch (tthread.getState()) {
		case TIMED_WAITING:
		case WAITING:
			tthread.interrupt();
			break;
		case TERMINATED:
			tthread = new TimerThread();
		case RUNNABLE:
		case NEW:
			tthread.start();
			break;
		default:
			System.err.println("UH oh timer is blocked");
		}
	}

	public boolean containsTask(TimerTask t) {
		return tasks.contains(t);
	}

	private class TimerThread extends Thread {
		public TimerThread() {
		}

		public void run() {
			while (state != Timer.State.STOPPED && !tasks.isEmpty()) {

				// Determines the soonest element
				long currentTime = System.currentTimeMillis();
				long soonest = Long.MAX_VALUE; // Start at +infinity so anything
												// is less

				synchronized (tasks) {
					Iterator<TimerTask> iter = tasks.listIterator();
					TimerTask t = (iter.hasNext()) ? iter.next() : null;

					for (; iter.hasNext(); t = iter.next()) {
						long endTime = t.getEndTime();
						if (endTime < currentTime) {
							t.activate();
							iter.remove();
						} else if (endTime < soonest)
							soonest = endTime;
					}

					try {
						Thread.sleep((soonest - currentTime) / 2);
					} catch (InterruptedException e) {
					} // Ignored because on interrupt it double checks state
				}
			}

			state = Timer.State.STOPPED;
		}
	}

	public String toString() {
		return "Timer{" + state + ", " + tasks.toString() + ", "
				+ tthread.toString() + "}";
	}
}