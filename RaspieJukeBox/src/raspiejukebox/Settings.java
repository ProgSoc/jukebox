package raspiejukebox;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple settings parser An entry uses the syntax of
 * \\s*[a-zA-Z][a-zA-Z0-9_]+\\s*=.+ (id=value).
 * 
 * If it is not an entry then it is considered a comment.
 * 
 * If you notice an abrupt exit on startup, it was probably a call to
 * getProperty with allow null set to false.
 * 
 * This is probably the most solid code of the whole juke box. 
 * 
 * @author Jableader
 */
public final class Settings {
	private static final String SETTINGS_PATH = "settings.ini";

	private Settings() {
	}

	private static Map<String, String[]> settings;

	static {
		reload();
	}

	public static void reload() {
		if (settings == null)
			reloadDatabase();
		else
			synchronized (settings) {
				reloadDatabase();
			}
	}

	/** Non thread-safe */
	private static void reloadDatabase() {
		File settingsFile = new File(SETTINGS_PATH);

		if (!settingsFile.exists()) {
			Log log = LogFactory.getLog(Settings.class);

			log.error("No settings file found at "
					+ settingsFile.getAbsolutePath());
			System.exit(-1);
		}

		Scanner fileReader = null;

		try {
			fileReader = new Scanner(new FileReader(settingsFile));
			settings = new ConcurrentHashMap<String, String[]>();
			while (fileReader.hasNext()) {
				String nextLine = fileReader.nextLine();

				//Skips an invalid line
				if (!nextLine.matches("\\s*[a-zA-Z][a-zA-Z0-9_]+\\s*=.+"))
					continue;

				String[] s = nextLine.split("=");

				String key = s[0].trim().toLowerCase();
				String[] value;

				if (settings.containsKey(key)) {
					String[] currentArray = settings.get(key);

					value = new String[currentArray.length + 1];
					for (int j = 0; j < currentArray.length; j++)
						value[j] = currentArray[j];

				} else
					value = new String[1];

				value[value.length - 1] = s[1];

				settings.put(key, value);
			}
		} catch (IOException e) {
			Log log = LogFactory.getLog(Settings.class);
			log.error("There was an error reading the settings file. System will close");

			System.exit(-1);
		} finally {
			if (fileReader != null)
				fileReader.close();
		}
	}

	/**
	 * Returns an array of all values with the given identifier If nothing was
	 * in the settings file with the given identifier it returns null
	 * 
	 * @param id
	 *            the id for the property
	 */
	public static String[] getProperty(String id) {
		return settings.get(id.toLowerCase());
	}

	/**
	 * Returns an array of all values with the given identifier, parsed into the
	 * given type.
	 * 
	 * The legal types are : Integer, Double, Short, Long, Byte, Float
	 * 
	 * It throws an IllegalArguementException if the given type is not legal. It
	 * throws a NumberFormatException where a value cannot be properly parsed
	 * 
	 * @param id
	 *            The String id for the given property
	 * @param type
	 *            The type to coerce the returned array into
	 * @return An array of type[] with all values with the ID id, or null where
	 *         no values exist with the given id
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] getProperty(String id, Class<T> type) {
		String[] sProps = getProperty(id);

		if (sProps == null)
			return null;

		if (type == String.class)
			return (T[]) sProps;

		T[] tProps = (T[]) Array.newInstance(type, sProps.length);

		if (type == Integer.class)
			for (int j = 0; j < sProps.length; j++)
				tProps[j] = type.cast(Integer.parseInt(sProps[j].trim()));

		else if (type == Long.class)
			for (int j = 0; j < sProps.length; j++)
				tProps[j] = type.cast(Long.parseLong(sProps[j].trim()));

		else if (type == Short.class)
			for (int j = 0; j < sProps.length; j++)
				tProps[j] = type.cast(Short.parseShort(sProps[j].trim()));

		else if (type == Float.class)
			for (int j = 0; j < sProps.length; j++)
				tProps[j] = type.cast(Float.parseFloat(sProps[j].trim()));

		else if (type == Double.class)
			for (int j = 0; j < sProps.length; j++)
				tProps[j] = type.cast(Double.parseDouble(sProps[j].trim()));

		else if (type == Byte.class)
			for (int j = 0; j < sProps.length; j++)
				tProps[j] = type.cast(Byte.parseByte(sProps[j].trim()));

		else
			throw new IllegalArgumentException("Given type is not legal");

		return tProps;
	}

	/**
	 * Calls the getProperty() function and if it has an exception or returns
	 * null it logs an error and terminates the program
	 * 
	 * @param acceptNull
	 *            whether or not the function can return null
	 * @return An array of type[] with all values with the ID id
	 */
	public static <T> T[] getProperty(String id, Class<T> type,
			boolean acceptNull) {
		T[] result = null;

		try {
			result = getProperty(id, type);

		} catch (NumberFormatException ex) {
			Log log = LogFactory.getLog(Settings.class);

			log.error("Setting " + id + " could not be parsed into type "
					+ type.getName());
		}

		if (result == null && !acceptNull) {
			Log log = LogFactory.getLog(Settings.class);

			log.error("Setting " + id + " could not be found");
			log.error("Check your settings.txt file");
			System.exit(-1);
		}

		return result;
	}
}
