package testing;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import raspiejukebox.TracksDatabase;
import raspiejukebox.TracksDatabase.Track;
import static java.nio.file.StandardCopyOption.*;
import java.util.*;

/**Used to grab and organise tracks (by artist name) from an ipod*/
public class Organiser {

	public static final String OUT_DIR = "C:\\Music\\";

	public static String[] toArr(String... obj) {
		return obj;
	}

	public static void main(String[] args) {
		HashMap<String, String[]> desiredAlbums = new HashMap<String, String[]>();

		desiredAlbums.put("kimbra", null);

		TracksDatabase tdb = TracksDatabase.get();

		for (Map.Entry<String, String[]> es : desiredAlbums.entrySet()) {
			List<Track> disco = tdb.getTracksByArtist(es.getKey());
			if (disco == null)
				System.out.println("Can't find artist " + es.getKey());
			else {
				System.out.println("Doing " + es.getKey());
				String artistPath = OUT_DIR + es.getKey();
				File f = new File(artistPath);
				f.mkdirs();
				String[] albumsToCopy = es.getValue();
				if (albumsToCopy != null)
					for (String album : albumsToCopy) {
						String albumPath = artistPath + "\\" + album;
						f = new File(albumPath);
						f.mkdir();

						for (Track t : disco)
							try {
								if (album == null
										|| t.getAlbum().equalsIgnoreCase(album)) {
									Path from = t.getFile().toPath();
									File to = new File(artistPath + "\\"
											+ t.getAlbum());
									to.mkdirs();
									Path toPath = to.toPath().resolve(
											from.getFileName());
									if (!toPath.toFile().exists())
										Files.copy(from, toPath,
												REPLACE_EXISTING);
								}
							} catch (Exception e) {
								System.out.println(e.getMessage());
								e.printStackTrace();
							}
					}
				else {
					for (Track t : disco)
						try {

							Path from = t.getFile().toPath();
							File to = new File(artistPath + "\\" + t.getAlbum());
							to.mkdirs();
							Files.copy(from,
									to.toPath().resolve(from.getFileName()),
									REPLACE_EXISTING);

						} catch (Exception e) {
							System.out.println(e.getMessage());
							e.printStackTrace();
						}
				}
			}
		}
		System.out.println("Done");
	}
}
