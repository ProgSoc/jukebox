package testing;

import java.io.File;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

/**Reads and prints all ID3 tags for a track. Useful to determine what are valid tags and what
 * might be desirable.
 * 
 * @author Jableader
 *
 */
public class ID3Reader {
	public static void main(String[] args) throws Exception {

		AudioFileFormat aff = AudioSystem.getAudioFileFormat(new File(
				"C:\\Music\\Gorillaz\\Plastic Beach\\HYBR.mp3"));
		Map<String, Object> properties = aff.properties();

		for (Map.Entry<String, Object> e : properties.entrySet())
			System.out.println(String.format("%-10s = %s", e.getKey(),
					e.getValue()));
	}

}
