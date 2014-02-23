package data;

import raspiejukebox.*;
import java.util.*;

public class DataRequest {
	
	//Converts object to string
	public static CharSequence stringify(Object o){
		if (o instanceof String)
			return ((String)o).replaceAll("(\"|\\\\)", "\\\\$1")
								.replaceAll("^|$", "\"");
								
		else if (o instanceof TracksDatabase.Track){
			TracksDatabase.Track t = (TracksDatabase.Track)o;
			StringBuilder sb = new StringBuilder();
			sb.append("{'id':");
			sb.append(t.getID());
			sb.append(",'name':");
			sb.append(stringify(t.getName()));
			sb.append(",'album':");
			sb.append(stringify(t.getAlbum()));
			sb.append(",'artist':");
			sb.append(stringify(t.getArtist()));
			sb.append('}');
			
			return sb;
		} else if (o instanceof List){
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (Object oo: (List)o){
				sb.append(stringify(oo));
				sb.append(',');
			}
			
			sb.setLength(sb.length()-1);
			sb.append(']');
			return sb;
		}
		
		return "";
	}
	
	public static CharSequence getDiscography(String artistName){
		List<TracksDatabase.Track> discography = (artistName != null) ?
													TracksDatabase.get().getTracksByArtist(artistName) :
													null;
		if (discography == null)
			return "[]";
			
		return stringify(discography);
	}
	
	public static CharSequence getArtists(){
		return stringify(TracksDatabase.get().getArtists());	
	}
	
	public static CharSequence getAll(){
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (String artistName: TracksDatabase.get().getArtists()){
			sb.append(stringify(artistName));
			sb.append(':');
			sb.append(getDiscography(artistName));
			sb.append(',');
		}
		
		sb.setLength(sb.length()-1);
		sb.append('}');
		return sb;
	}
}
