package raspiejukebox;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;
import static java.util.Collections.sort;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The database of tracks. It is held in memory, and can be updated through a
 * refresh call.<br>
 * The module also maintains a cache so that load time is significantly reduced.<br>
 * If it is told to use the cache on startup, it will first try to load that,
 * then if it is successful it will refresh the database asynchronously (on a
 * new thread). It it fails then it will refresh the database before continuing.
 */
public class TracksDatabase {
	public static final String CACHE_LOCATION = "cachedb.ser";

	private List<String> artistNames; // Ordered list of artist names
	private List<Track> database; // The entire database (sorted)
	private static TracksDatabase instance; // The singleton instance
	private Map<String, List<Track>> artistDatabase; // The database of artists
														// & tracks
	
	static private boolean currentlyReloading = false; //Blegh shouldn't have used singleton
	
	
	// Class initialiser. Initialises the database and adds tracks from given
	// directories
	static {
		long nanoTime = System.nanoTime();
		String[] useCached = Settings.getProperty("useCachedDatabaseOnStartup");
		if (useCached == null || useCached[0].equalsIgnoreCase("false")
				|| !(new File(CACHE_LOCATION).exists())) {
			System.out.println("Loading Tracks without cache");
			reloadDatabase(false);
			System.out.println("Time Taken: " + (System.nanoTime() - nanoTime));

		} else {
			System.out.println("Loading tracks via cache");
			instance = getCachedDatabase();
			System.out.println("Done Loading Cache");
			reloadDatabase((instance != null)); // Loads async if cache
			// retrieval is successful
			System.out.println("Time Taken: " + (System.nanoTime() - nanoTime));
		}
	}

	private TracksDatabase(List<String> artists,
			Map<String, List<Track>> artistDatabase, List<Track> tracks) {
		this.artistNames = artists;
		this.artistDatabase = artistDatabase;
		this.database = tracks;
	}

	/**
	 * Very CPU intensive. Creates database, loads all tracks & ID3 tags and
	 * sorts database, as well as organising artists
	 */
	private static TracksDatabase getNewDatabase() {
		try {
			Collection<Track> db = new TreeSet<Track>();

			Stack<File> directories = new Stack<File>();
			String[] tracksDirectories = Settings.getProperty("tracksLocation",
					String.class, false);
			Log log = LogFactory.getLog(TracksDatabase.class);

			// Filter to only accept mp3, wav & ogg
			FilenameFilter trackFilter = new FilenameFilter() {
				public boolean accept(File parent, String name) {
					return (name.endsWith("mp3") || name.endsWith("wav") || name
							.endsWith("ogg"));
				}
			};

			FileFilter dirFilter = new FileFilter() {
				public boolean accept(File f) {
					return f.isDirectory();
				}
			};

			System.out.println("Loading Database");

			for (String tracksSource : tracksDirectories) {
				File f = new File(tracksSource);

				if (!f.exists() || f.isFile())
					log.error("Directory doesnt exist: " + f.getAbsoluteFile());
				else
					directories.push(f);
			}

			// Traverses the tree and finds all files, this means tracks can be
			// added in folders and manually organised
			// Can come in handy if we decide to organise everything in an
			// artist/album/track.mp3 format
			int tracksLoaded = 0;
			while (directories.size() > 0) {
				File dir = directories.pop();

				// Gets all the dirs
				File[] childDirs = dir.listFiles(dirFilter);
				if (childDirs != null) // This avoids ECT for if we dont have
					// permissions
					for (File d : dir.listFiles(dirFilter))
						directories.push(d);

				// Gets all the tracks
				File[] dirTracks = dir.listFiles(trackFilter);
				if (dirTracks != null)
					for (File f : dir.listFiles(trackFilter))
						try {
							Track t = new Track(f);
							db.add(t);
							if (++tracksLoaded % 500 == 0)
								System.out.println(tracksLoaded
										+ " tracks loaded");
						} catch (IOException e) {
							log.error("Unable to load " + f.getAbsolutePath());
						} catch (UnsupportedAudioFileException e) {
							log.error("Unsupported audio format "
									+ f.getAbsolutePath());
						} catch (NullPointerException ne) {
							log.error("WTF null pointer in tracksdb loadup");
							ne.printStackTrace();
						}

			}

			List<Track> database = new ArrayList<Track>(db.size());

			System.out.println("Setting IDs");
			// Resetting all the IDs to match the array
			int j = -1;
			for (Track t : db) {
				t.id = ++j;
				database.add(t);
			}

			db = null;

			// Creating the artists database
			Map<String, List<Track>> artistDatabase = new HashMap<String, List<Track>>();

			System.out.println("Creating ArtistDB");

			// Duplicates sections of the list for the artist
			for (int bottom = 0; bottom < database.size();) {
				String artistName = database.get(bottom).getArtist();
				int top = bottom;
				while (++top < database.size() // find last file with the
						// same artist
						&& artistName.equals(database.get(top).getArtist()))
					;

				List<Track> tracks = database.subList(bottom, top);

				artistDatabase.put(artistName, tracks);
				bottom = top;
			}

			List<String> artistNames = new ArrayList<String>();
			artistNames.addAll(artistDatabase.keySet());
			sort(artistNames);

			System.out.println("Database Loaded");
			return new TracksDatabase(artistNames, artistDatabase, database);
		} catch (Exception e) {
			System.out.println("Database is fucked: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/** Loads a cached version of the database. Compressing as it loads */
	private static TracksDatabase getCachedDatabase() {
		// Protocol: (int size)((String Artist)(Int discoSize)(Track
		// track){discoSize}){size} (List<String artistNames)

		ObjectInputStream input = null;
		TracksDatabase returnValue = null; // The value to return

		try {
			FileInputStream fis = new FileInputStream(CACHE_LOCATION);
			BufferedInputStream bis = new BufferedInputStream(fis);
			input = new ObjectInputStream(bis); // Creating a buffered
												// ObjectInputStream

			int numberOfTracks = input.readInt();
			System.out.println("Loading " + numberOfTracks + " tracks");
			List<Track> trackdb = new ArrayList<Track>(numberOfTracks);
			HashMap<String, List<Track>> artistMap = new HashMap<String, List<Track>>();

			int tracksRead = 0;
			while (tracksRead < numberOfTracks) {
				String artistName = (String) input.readObject();
				int tracksForArtist = input.readInt();

				List<Track> discography = new ArrayList<Track>(tracksForArtist);

				for (int j = 0; j < tracksForArtist; j++) {
					Track t = (Track) input.readObject();
					t.artist = artistName;
					discography.add(t);
				}

				artistMap.put(artistName, discography);
				trackdb.addAll(discography);

				tracksRead += tracksForArtist;
			}

			List<String> artistNames = (List<String>) input.readObject();
			returnValue = new TracksDatabase(artistNames, artistMap, trackdb);

			input.close();

		} catch (Exception e) {
			System.out.println("There was an error loading the cache");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		return returnValue;
	}

	/** Saves a given database to the cache */
	private static void saveDatabase(TracksDatabase tdb) {
		ObjectOutputStream output = null;
		boolean error = false;
		try {
			output = new ObjectOutputStream(new BufferedOutputStream(
					new FileOutputStream(CACHE_LOCATION, false)));

			int dbSize = tdb.database.size();
			output.writeInt(dbSize);
			int tracksForArtist = 0;
			for (int savedTracks = 0; savedTracks < dbSize; savedTracks += tracksForArtist) {
				String artistName = tdb.database.get(savedTracks).artist;
				tracksForArtist = tdb.artistDatabase.get(artistName).size();

				output.writeObject(artistName);
				output.writeInt(tracksForArtist);

				for (int j = savedTracks; j < savedTracks + tracksForArtist; j++)
					output.writeObject(tdb.database.get(j));
			}

			output.writeObject(tdb.artistNames);
		} catch (Exception e) {
			e.printStackTrace();

			error = true;
		}

		try {
			if (output != null)
				output.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		if (error) {
			File f = new File(CACHE_LOCATION);
			if (f.exists())
				f.delete();
		}
	}

	/** Reloads a database from settings trackspath directories */
	public static void reloadDatabase(boolean async) {
		final Runnable loadDatabaseRunnable = new Runnable() {
			public void run() {
				currentlyReloading = true;
				TracksDatabase newDB = getNewDatabase();

				if (instance == null)
					instance = newDB;
				else
					synchronized (instance) {
						instance = newDB;
					}
				currentlyReloading = false;
			}
		};

		final Runnable saveDatabaseRunnable = new Runnable() {
			public void run() {
				System.out.println("Saving new database to cache");
				saveDatabase(instance);
				System.out.println("Done saving new database");
			}
		};

		if (async) {
			new Thread() {
				@Override
				public void run() {
					loadDatabaseRunnable.run();
					saveDatabaseRunnable.run();
				}
			}.start();
		} else {
			loadDatabaseRunnable.run();
			new Thread(saveDatabaseRunnable).start();
		}
	}

	/**
	 * Gets an instance of the tracks database
	 */
	public static TracksDatabase get() {
		return instance;
	}

	/** Returns the array of tracks of the database */
	public List<Track> getTracks() {
		return database;
	}

	/** Gets all the tracks for a given artist */
	public List<Track> getTracksByArtist(String artist) {
		return artistDatabase.get(artist);
	}

	public List<String> getArtists() {
		return artistNames;
	}

	public int getSize() {
		return database.size();
	}

	/** Gets a random track from the database */
	public Track getRandomTrack() {
		return database.get((int) (Math.random() * database.size()));
	}

	public Track getTrackByID(int id) {
		if (id >= database.size())
			return null;
		else
			return database.get(id); //An ID is its index inthe array
	}
	
	/**Returns whether or not the db is currently reloading the database
	 * 
	 */
	public boolean isReloading(){
		return currentlyReloading;
	}

	// Immutable track
	public static class Track implements Comparable<Track>, Serializable {
		private static final long serialVersionUID = 8601568701806126212L;

		public static final String UNKNOWN = "Unknown";

		private int trackNum;
		private String name;
		private String artist;
		private String album;
		private String path;
		private int id;

		private static int lastID = 0;

		public Track(File f) throws IOException, UnsupportedAudioFileException {
			path = f.getAbsolutePath();
			AudioFileFormat aff = AudioSystem.getAudioFileFormat(f);
			Map<String, Object> properties = aff.properties();

			Object tArtist, tName, tAlbum; // Temporaries

			tName = fix(properties.get("title")); // t(emporary)Name
			tArtist = fix(properties.get("mp3.id3tag.orchestra"));
			if (tArtist == null)
				tArtist = fix(properties.get("author"));
			tAlbum = fix(properties.get("album"));

			// Track number is a pain in the ass, comes in form (eg) 8/12
			try {
				trackNum = Integer.parseInt(((String) properties
						.get("mp3.id3tag.track")).split("/")[0]);

				// Possible exceptions: NumberFormatException,
				// NullPointerException
			} catch (Exception e) {
				trackNum = 0;
			}
			name   = (String) (tName == null   ? f.getName() : tName);
			artist = (String) (tArtist == null ? UNKNOWN : tArtist);
			album  = (String) (tAlbum == null  ? UNKNOWN : tAlbum);

			id = lastID++;
		}

		@Override
		public String toString() {
			return String.format("Track{%s, %s, %s @%s}", artist, album, name,
					path);
		}

		/**Returns either the trimmed string or null if it is empty or null. Basically null-proofing */
		private String fix(Object s) {
			if (s == null || !(s instanceof String)) return null;
			String rs = ((String)s).trim();
			return (rs.length() == 0) ? null : rs;
		}

		public String getName() {
			return name;
		}

		public String getArtist() {
			return artist;
		}

		public String getAlbum() {
			return album;
		}

		public int getTrackNumber() {
			return trackNum;
		}

		public File getFile() {
			return new File(path);
		}

		public String getFilePath() {
			return path;
		}

		public long getID() {
			return id;
		}

		public int compareTo(Track t) {
			if (t == this)
				return 0;

			int res = artist.compareTo(t.artist);

			if (res == 0) {
				res = album.compareTo(t.album);

				if (res == 0) {
					res = (int) Math.signum(trackNum - t.trackNum);

					if (res == 0)
						res = name.compareTo(t.name);
				}
			}

			return res;
		}
	}
}