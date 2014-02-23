<%@ page contentType="text/xml" import="raspiejukebox.*, java.io.*, java.util.List"%>

<%!
String getVolume(){
	try{
		Process proc = Runtime.getRuntime().exec("amixer get PCM");
		BufferedReader stdIn = new BufferedReader(new 
					 InputStreamReader(proc.getInputStream()));
		
		String volume = "";
		String line = null;
		while ((line = stdIn.readLine()) != null && !volume.matches("[0-9]+\\%"))
			volume = line.replaceAll(".*?([0-9]+\\%).*", "$1");
		
		return volume;
	} catch (Exception e) {
		//out.println("<!--IO error occured-->");
		return "00%";
	}
}%>

<%!
String toXml(String s){
	return s.replace("\"", "&quot;").replace("&", "&amp;");
}

void printTrackToXml(TracksDatabase.Track t, Writer outStream) throws IOException{
	outStream.append("<track id=\"");
	outStream.append(Long.toString(t.getID()));
	outStream.append("\">");
	outStream.append("<name>");
	outStream.append(toXml(t.getName()));
	outStream.append("</name>");
	outStream.append("<artist>");
	outStream.append(toXml(t.getArtist()));
	outStream.append("</artist>");
	outStream.append("<album>");
	outStream.append(toXml(t.getAlbum()));
	outStream.append("</album>");
	outStream.append("</track>");
}
%>

<%
JukeBox jb = JukeBox.get();
TracksDatabase tdb = TracksDatabase.get();
String data = request.getParameter("data");

try{
	if (data == null)
		;//Do Nothing
	else if (data.equals("tracks")){
%><tracks><%
		for (TracksDatabase.Track t: tdb.getTracks())
			printTrackToXml(t, out);
%></tracks><%
	}
	else if (data.equals("discography")){
		String artistName = request.getParameter("artistname");
		List<TracksDatabase.Track> discography = (artistName == null) ? null : tdb.getTracksByArtist(artistName);
		if (discography != null){%>
			<discography artist="<%=artistName%>"><%
			for (TracksDatabase.Track t: discography)
				printTrackToXml(t, out);
			%></discography><%
		} else {%>
			<discography />
		<%}
	}
			
	else if (data.equals("artists"))
		for (String artistName: tdb.getArtists()){
			out.print("<artist>");
			out.print(artistName);
			out.print("</artist>");
		}
		
	else if (data.equals("queue")){
	%>	<queue> <%
		for (TracksDatabase.Track t: jb.getQueue())
			printTrackToXml(t, out);
	%>	</queue> <%
	}
	
	else if (data.equals("currentstate")){
		%><state><%
		TracksDatabase.Track currentTrack = jb.getCurrentTrack();
		if (currentTrack != null) printTrackToXml(currentTrack, out);
		else { //Blank Track
			%>
			<track id="-1">
				<name>Nothing!</name>
				<album>--</album>
				<artist>--</artist>
			</track>
			<%
		}
		%><volume><%=getVolume()%></volume><%
		%></state><%
	}
} catch (IOException ioe){
	System.err.println(ioe.getMessage());
}
%>
