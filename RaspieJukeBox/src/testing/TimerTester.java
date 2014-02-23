package testing;

import timer.*;

/** A simple class made simply to test the Timers */
public class TimerTester {

	public static void main(String[] args) {
		Timer t = new Timer();
		for (int j = 1; j < 11; j++)
			t.addTask(new Countdown(j));

		t.addTask(new TimerTask(System.currentTimeMillis() + 11 * 1000,
				new TimerEventListener() {
					public void endTimeAchieved(TimerEvent ev) {
						System.exit(0);
					}

					public void timerInterrupted(TimerEvent ev) {
					}
				}));

		try {
			Thread.sleep(30000L);
		} catch (InterruptedException e) {
		}
	}

	private static class Countdown extends TimerTask {

		public Countdown(final int seconds) {
			super(System.currentTimeMillis() + seconds * 1000L,
					new TimerEventListener() {

						public void endTimeAchieved(TimerEvent ev) {
							System.out
									.println(seconds + " seconds have passed");
						}

						public void timerInterrupted(TimerEvent ev) {
							// TODO Auto-generated method stub

						}
					});
		}
	}
}
