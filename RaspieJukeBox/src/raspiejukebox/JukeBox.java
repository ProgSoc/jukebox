package raspiejukebox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import raspiejukebox.TracksDatabase.Track;
import locks.*;

/**
 * The Music player module. This controls all the music playback, queing and
 * song selection.<br>
 * If no track is in the que, will play random songs from the tracks database.<br>
 * it is a singleton, call the get() function to get the instance.
 * 
 * @author Jableaderst
 */
public class JukeBox implements Lockable {

	// You will notice most public void functions are overloaded to a private
	// function with a key,
	// This is to allow for locking to make the object mutable externally, yet
	// still allow
	// the functions to be mutable for calls within the module.

	public enum State {
		PLAYING, PAUSED, STOPPED, ERROR
	}

	private static final int PLAYBACK_ERROR_MAX = Settings.getProperty(
			"maxPlaybackError", Integer.class, false)[0];
	private static JukeBox instance;

	private List<JukeBoxQueueListener> queueListeners;
	private List<JukeBoxStateListener> stateListeners;
	private State state;
	private MusicPlayer musicPlayer = new MusicPlayer(); // does the grunt
	// playing music
	private Queue<Track> songQueue;

	private JukeboxKey currentKey = null;

	private Log log = LogFactory.getLog(JukeBox.class);

	static {
		instance = new JukeBox();
	}

	private JukeBox() {
		songQueue = new ConcurrentLinkedQueue<Track>();
		stateListeners = new ArrayList<JukeBoxStateListener>(2);
		queueListeners = new ArrayList<JukeBoxQueueListener>(2);
		setState(State.STOPPED);
	}

	/**
	 * Gets the singleton instance of the jukebox.
	 * 
	 * @return The Jukebox
	 */
	public static JukeBox get() {
		return instance;
	}

	private synchronized void pause(Key k) {
		if ((k == currentKey) || (k != null && k.equals(currentKey))) {
			try {
				if (state == State.PLAYING) {
					musicPlayer.pause();
					setState(State.PAUSED);
				}
			} catch (BasicPlayerException e) {
				log.error("Error pausing " + musicPlayer.getTrack().getName()
						+ "\n\t" + e.getMessage());
			}
		}
	}

	/** Pause the jukebox (if playing) */
	public void pause() {
		pause(null);
	}

	private void play(Key k) {
		if (!((k == currentKey) || (k != null && k.equals(currentKey))))
			return;
		try {
			switch (musicPlayer.getStatus()) {
			case BasicPlayer.PAUSED:
			case BasicPlayer.OPENED:
				musicPlayer.play();
				setState(State.PLAYING);
				break;

			case BasicPlayer.PLAYING:
				// Do Nothing, we're already playing
				break;
			default:
				playNext(currentKey);
			}

		} catch (BasicPlayerException ex) {
			log.error("Error playing " + musicPlayer.getTrack().getName()
					+ "\n\t: " + ex.getMessage());
			playNext(currentKey);
		}
	}

	/**
	 * Plays the jukebox.<br>
	 * If it is paused, plays from paused point. If stopped, plays next song in
	 * the queue.<br>
	 * If it errors it skips to the next track
	 * */
	public void play() {
		play(null);
	}

	private void fireQueueStateChange(){
		JukeBoxEvent e = new JukeBoxEvent(this, JukeBoxEvent.Type.QUEUE);
		for (JukeBoxQueueListener ql: queueListeners)
			ql.queueChanged(e);
	}
	
	/** Adds the given track to the end of the queue */
	public void addToQueue(Track t) {
		songQueue.offer(t);
		fireQueueStateChange();
	}

	private void playNext(Key k) {
		// Many apologies for this awful spaghetti code
		if (!((k == currentKey) || (k != null && k.equals(currentKey))))
			return;

		// Used later to determine whther or not to play the next track
		// If musicPlayer.getTrack == null, then we are just starting up.
		// Without that you must press play twice
		boolean continuePlaying = (state == State.PLAYING || musicPlayer
				.getTrack() == null);

		boolean error = true; // Apon encounting an error, it will skip till
		// success or max error
		int errorCount = 0;
		Track nextTrack = null;
		if (songQueue.isEmpty())
			nextTrack = TracksDatabase.get().getRandomTrack();
		else {
			nextTrack = songQueue.poll();
			fireQueueStateChange();
		}

		do
			try { // In the try we try to set the track, then play
				musicPlayer.setTrack(nextTrack);

				if (continuePlaying) {
					musicPlayer.play();
					setState(State.PLAYING);
				}

				error = false;

			} catch (BasicPlayerException e) {
				// upon error we retry this song, then if it fails twice we try
				// a new song
				// until either success or it reaches a max error count
				
				errorCount++;
				error = true;

				System.out.println(e.getMessage());
				System.out.println(e.getCause());
				e.printStackTrace();
				
				if (errorCount >= PLAYBACK_ERROR_MAX) {
					setState(State.ERROR);
					System.err.println("Entering error mode");
					return; // Cannot play, reverting to error state

				}
				else if ((errorCount %3 % 2) == 0)
					reset();
				else if (errorCount % 3 == 0) // try every track twice, then reset the track
					if (songQueue.isEmpty())
						TracksDatabase.get().getRandomTrack();
					else {
						songQueue.poll();
						fireQueueStateChange();
					}
				
			}
		while (error);
	}

	/**
	 * Stops playback of current song (if needed) and plays next in the queue.<br>
	 * It skips if there is an error, and continues until either a song worked
	 * or an max is reached (The max is determined via the settings file). <br>
	 * If the music was already paused, it will load the next track without
	 * playing it
	 */
	public void playNext() {
		playNext(null); // Null would mean it's unlocked anyway
	}

	private void stop(Key k) {
		if (((k == currentKey) || (k != null && k.equals(currentKey))))
			try {
				musicPlayer.stop();
				setState(State.STOPPED);
			} catch (BasicPlayerException ex) {
				log.error("Error: You can't stop the music");
			}
	}

	/** Stops playing music, and also deallocates resources */
	public void stop() {
		stop(null);
	}

	private void reset(Key k) {
		if (((k == currentKey) || (k != null && k.equals(currentKey)))) {
			songQueue.clear();
			fireQueueStateChange();
			stop();
			musicPlayer = new MusicPlayer();
		}
	}

	/** Stops the player and empties the queue, making it appear as new */
	public void reset() {
		reset(null);
	}

	/** Gets the current state of the jukebox */
	public State getState() {
		return state;
	}

	/** Sets the state and calls the same listeners */
	private void setState(State s) {
		state = s;
		JukeBoxEvent jbe = new JukeBoxEvent(this, JukeBoxEvent.Type.STATE);
		for (JukeBoxStateListener l : stateListeners)
			l.stateChanged(jbe);
	}

	/**
	 * Gets a read only version of the current queue. <br>
	 * Does not include the currently playing track, the next track to play is
	 * at index 0. <br>
	 * If queue is empty, returns empty array
	 */
	public Collection<Track> getQueue() {
		return songQueue;
	}

	public void addStateChangeListener(JukeBoxStateListener jbl) {
		if (jbl == null)
			throw new IllegalArgumentException("Listener cannot be null");
		stateListeners.add(jbl);
	}

	public void addQueueChangeListener(JukeBoxQueueListener jbl){
		if (jbl == null)
			throw new IllegalArgumentException("Listener cannot be null");
		queueListeners.add(jbl);
	}
	
	/** Gets the currently playing track */
	public Track getCurrentTrack(){
		return musicPlayer.getTrack();
	}
	
	public float getVolume(){
		return musicPlayer.getVolume();
	}
	
	public void setVolume(float val){
		musicPlayer.setVolume(val);
	}

	/**
	 * A simple wrapper to the BasicPlayer type that allows compatability with
	 * TracksDatabase.Track
	 * 
	 * @author Jableader
	 */
	private class MusicPlayer {
		private float volume = 0.5F;
		private Track track;
		BasicPlayer player;

		public MusicPlayer() {
			player = new BasicPlayer();

			// Below adds a player listener that plays the next track when the
			// current one finishes
			player.addBasicPlayerListener(new BasicPlayerListener() {

				public void stateUpdated(BasicPlayerEvent e) {
					if (e.getCode() == BasicPlayerEvent.EOM)
						playNext(currentKey);
				}

				@SuppressWarnings(value = { "rawtypes" })
				public void opened(Object arg0, Map arg1) {
				}

				@SuppressWarnings(value = { "rawtypes" })
				public void progress(int arg0, long arg1, byte[] arg2, Map arg3) {
				}

				public void setController(BasicController arg0) {
				}
			});

			// Disable logging
			// Logger.getLogger(BasicPlayer.class.getName()).setLevel(Level.OFF);
		}

		/** Sets the current track. Stops and frees up resources from old one */
		public void setTrack(Track t) throws BasicPlayerException {
			track = t;
			player.open(t.getFile());
		}

		/** Stops track and frees up resources */
		public void stop() throws BasicPlayerException {
			player.stop();
			track = null;
		}

		/** Plays (or resumes) currently loaded track */
		public void play() throws BasicPlayerException {
			if (player.getStatus() == BasicPlayer.PAUSED)
				player.resume();
			else
				player.play();
			player.setGain(volume);
		}

		/** Pauses current track */
		public void pause() throws BasicPlayerException {
			player.pause();
		}

		/** Gets the status of the BasicPlayer */
		public int getStatus() {
			return player.getStatus();
		}

		public Track getTrack() {
			return track;
		}
		
		public float getVolume(){
			return volume;
		}
		
		public void setVolume(float val){
			try {
				player.setGain(val);
				volume = val;
			} catch (BasicPlayerException e){
				System.out.println("There was an error setting the volume");
			}
		}
	}

	/**
	 * Simple key using a serial of a random long
	 * 
	 * @author Jableader
	 * 
	 */
	private class JukeboxKey implements Key {
		private Long id;

		public JukeboxKey() {
			this((long) (Math.random() * (Long.MAX_VALUE
					+ Math.abs(Long.MIN_VALUE) + Long.MIN_VALUE)));
		}

		private JukeboxKey(long id) {
			this.id = id;
		}

		@Override
		public JukeboxKey clone() {
			return new JukeboxKey(id);
		}
	}

	public Key lock() {
		if (isLocked())
			return null;
		else {
			currentKey = new JukeboxKey();
			return currentKey;
		}
	}

	public void unlock(Key k) {
		if (k instanceof JukeboxKey && ((JukeboxKey) k).id == currentKey.id)
			currentKey = null;
		else
			throw new IllegalArgumentException("Key does not match lock");
	}

	public boolean isLocked() {
		return currentKey != null;
	}
}