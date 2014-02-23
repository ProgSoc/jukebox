package raspiejukebox;

import timer.*;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import locks.*;

/**
 * An interface specifically for hardware interaction, including buttons and
 * LEDs. May contain a lock on the jukebox.
 * 
 * @author Jableader
 */
public class HardwareInterface {
	private HardwareInterface() {
	}// Entirely static (so kill me)

	private static Key jukeboxKey = null; // Key for jukebox, if jukebox isn't
											// locked the key is null
	private static DeadManButton deadManButton;
	private static boolean isInitialised;
	private static GpioPinDigitalOutput statLed = null;

	public static void initialise() {
		if (Settings
				.getProperty("enableHardwareInterface", String.class, false)[0]
				.equals("true")) {
			deadManButton = new DeadManButton(); // Reserve the dead man button,
													// as it will be needed for
													// resetTimer call
			Button[] btns = { new PlayPauseButton(), new SkipButton(),
					deadManButton };

			GpioController gpio = GpioFactory.getInstance();

			for (Button b : btns)
				// Enabling all the buttons
				if (b.isEnabled()) {
					GpioPinDigitalInput raspiBtn = gpio
							.provisionDigitalInputPin(b.getPin(),
									PinPullResistance.PULL_DOWN);
					raspiBtn.addListener(b);
				}

			// The status LED, (playing | stopped => solid), (paused => slow
			// blink), (error => fast blink)
			Integer[] statusLEDPin = Settings.getProperty("statusLEDPin",
					Integer.class);
			if (statusLEDPin != null) {
				statLed = gpio
						.provisionDigitalOutputPin(getPinByNum(statusLEDPin[0]));
				statLed.high(); // Pulls high as soon as initialized

				JukeBox.get().addStateChangeListener(new JukeBoxStateListener() {
					public void stateChanged(JukeBoxEvent e) {
						JukeBox.State s = e.getState();
						if (s == JukeBox.State.PLAYING
								|| s == JukeBox.State.STOPPED) {
							statLed.blink(0); // turns off blink
							statLed.high();
						} else if (s == JukeBox.State.PAUSED)
							statLed.blink(500);
						else if (s == JukeBox.State.ERROR)
							statLed.blink(150);
					}

				});

				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						statLed.setState(false);
					}
				});
			}
		}

		isInitialised = true;
	}

	public static boolean isInitialised() {
		return isInitialised;
	}

	/**
	 * A simple wrapper for a button. The point of the wrappper is to allow
	 * conveniance for things like debounce. A button describes a pin and an
	 * action
	 * 
	 * @author Jableader
	 * 
	 */
	private static abstract class Button implements GpioPinListenerDigital {
		protected Pin pin = null;
		protected long lastTime = 0;
		public static int DEBOUNCE_TIMEOUT = 500; // Excessive timeout, but
													// definantly not gunna
													// error

		/**
		 * Will be called whn the button is pushed down. Automatically debounced
		 * and resets timers
		 */
		public abstract void onPressed();

		public void handleGpioPinDigitalStateChangeEvent(
				GpioPinDigitalStateChangeEvent e) {
			// De-bounce and also unlock & reset
			// deadMan (if needed) before calling
			// the onlow method

			long currentTime = System.currentTimeMillis();
			if (lastTime + DEBOUNCE_TIMEOUT <= currentTime) {
				if (e.getState() == PinState.HIGH) {
					if (jukeboxKey != null) {
						JukeBox.get().unlock(jukeboxKey);
						jukeboxKey = null;
						if (deadManButton.isEnabled())
							deadManButton.resetTimers();
					}

					onPressed();
				}
				lastTime = currentTime;
			}
		}

		/** Returns the pin for this button, or null if it is not enabled */
		public Pin getPin() {
			return pin;
		}

		/** Whether or not this button is enabled */
		public boolean isEnabled() {
			return pin != null;
		}
	}

	/**
	 * The button behind the play/pause action. Uses settings property
	 * "playPauseButtonPin"
	 */
	private static class PlayPauseButton extends Button {
		public PlayPauseButton() {
			Integer[] pin = Settings.getProperty("playPauseButtonPin",
					Integer.class);
			if (pin != null && !pin[0].equals(-1))
				this.pin = getPinByNum(pin[0]);
		}

		@Override
		public void onPressed() {
			JukeBox jb = JukeBox.get();

			if (jb.getState() == JukeBox.State.PAUSED)
				jb.play();
			else
				jb.pause();
		}
	}

	/**
	 * Calls the playNext function on the jukebox. VERY tempermental.
	 * 
	 * @author Jableader
	 */
	private static class SkipButton extends Button {
		public SkipButton() {
			Integer[] pin = Settings
					.getProperty("skipButtonPin", Integer.class);
			if (pin != null && !pin[0].equals(-1))
				this.pin = getPinByNum(pin[0]);
		}

		@Override
		public void onPressed() {
			//JukeBox jb = JukeBox.get();
			//jb.playNext();
			
			//An unfortunate issue with bounce and the PI made this the best working approach.
			//It actually just queries the webpage rather than accessing the jukebox directly
			testing.WebReader.getPage("http://localhost:8080//jukebox.jsp?com=next");
		}
	}

	/**
	 * Resets the deadMan timers. Also calls play
	 * 
	 * @author Jableader
	 */
	private static class DeadManButton extends Button {
		private long pauseTimeout, stopTimeout;
		private TimerTask pauseTask, stopTask;
		private Timer timer;

		public DeadManButton() {
			Integer[] pin = Settings.getProperty("deadManButtonPin",
					Integer.class);
			if (pin != null && !pin[0].equals(-1)) {
				this.pin = getPinByNum(pin[0]);

				timer = new Timer();

				Long[] pauseTimeout = Settings.getProperty("dmPauseTimeout",
						Long.class);
				if (pauseTimeout != null && !pauseTimeout[0].equals(-1)) {
					this.pauseTimeout = pauseTimeout[0] * 1000;
					pauseTask = new TimerTask(0, new TimerEventListener() {
						public void endTimeAchieved(TimerEvent ev) {
							JukeBox jb = JukeBox.get();
							jb.pause();
							jukeboxKey = jb.lock();
						}

						public void timerInterrupted(TimerEvent ev) {
						}
					});

					timer.addTask(pauseTask);
				}

				Long[] stopTimeout = Settings.getProperty("dmStopTimeout",
						Long.class);
				if (stopTimeout != null && !stopTimeout[0].equals(-1)) {
					this.stopTimeout = stopTimeout[0] * 1000;
					stopTask = new TimerTask(0, new TimerEventListener() {

						public void endTimeAchieved(TimerEvent ev) {
							JukeBox jb = JukeBox.get();

							jb.unlock(jukeboxKey);
							jukeboxKey = null;
							jb.reset();
						}

						public void timerInterrupted(TimerEvent ev) {
						}
					});

					timer.addTask(stopTask);
				}
			}
		}

		@Override
		public void onPressed() {
			JukeBox jb = JukeBox.get();

			// Mostly takes advantage of the conveniance of the onLow method
			// resetting all the timers anyway

			jb.play();
		}

		public void resetTimers() {
			long currentTime = System.currentTimeMillis();

			if (pauseTask != null) { // If it is enabled
				pauseTask.setEndTime(currentTime + pauseTimeout); // reset it

				if (!timer.containsTask(pauseTask)) // if it has expired
					timer.addTask(pauseTask); // re-add it
			}

			// Identical but for stop
			if (stopTask != null) {
				stopTask.setEndTime(currentTime + stopTimeout);

				if (!timer.containsTask(stopTask))
					timer.addTask(stopTask);
			}
		}
	}

	private static Pin getPinByNum(int num) {
		switch (num) {
		case 0:
			return RaspiPin.GPIO_00;
		case 1:
			return RaspiPin.GPIO_01;
		case 2:
			return RaspiPin.GPIO_02;
		case 3:
			return RaspiPin.GPIO_03;
		case 4:
			return RaspiPin.GPIO_04;
		case 5:
			return RaspiPin.GPIO_05;
		case 6:
			return RaspiPin.GPIO_06;
		case 7:
			return RaspiPin.GPIO_07;
		case 8:
			return RaspiPin.GPIO_08;
		case 9:
			return RaspiPin.GPIO_09;
		case 10:
			return RaspiPin.GPIO_10;
		case 11:
			return RaspiPin.GPIO_11;
		case 12:
			return RaspiPin.GPIO_12;
		case 13:
			return RaspiPin.GPIO_13;
		case 14:
			return RaspiPin.GPIO_14;
		case 15:
			return RaspiPin.GPIO_15;
		case 16:
			return RaspiPin.GPIO_16;
		case 17:
			return RaspiPin.GPIO_17;
		case 18:
			return RaspiPin.GPIO_18;
		case 19:
			return RaspiPin.GPIO_19;
		case 20:
			return RaspiPin.GPIO_20;
		default:
			return null;
		}

	}
}
