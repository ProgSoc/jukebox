<!DOCTYPE html
	PUBLIC "-//W3D//DTD XHTML 1.0 Strict//EN"
		"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%@page import="raspiejukebox.*, java.util.List, java.net.URLEncoder"%>

<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
<link rel="stylesheet" type="text/css" href="basicstyle.css" />
<script src="xmlfunctions.js" type="text/javascript"></script>
</head>

<%
TracksDatabase tdb = TracksDatabase.get();

String songToAdd = request.getParameter("addsong");
if (songToAdd != null)
	JukeBox.get().addToQueue(tdb.getTrackByID(Integer.parseInt(songToAdd)));

String artistName = (String)request.getParameter("artistname");

if (artistName == null) { %>

<body>
	<div style="padding-top:0.5em; padding-bottom:0.5em; font-size:1.75em; padding-left:0.5em;">Artists</div>
	<table cellspacing="0" style="width:100%">
		<%
		int i = 0;
		String rowFormat = "<tr class=\"d%d selectable\" onClick=\"document.location='trackselect.jsp?artistname=%s'\"><td style=\"border-right:none\">%s</td></tr>";


		for (String artist: tdb.getArtists())
			out.print(String.format(rowFormat, ++i%2, URLEncoder.encode(artist, "UTF-8"), artist));
		%>
	</table>
</body>

<%} else {%>

<body>
	<div style="padding-top: 0.5em; padding-bottom: 0.5em">
		<span style="font-size:1.75em; padding-left:0.5em;"><%=artistName%></span>
		<span class="selectable" style="padding-left:3em;" onClick="history.back();">Back</span>
	</div>
	
	<table cellspacing="0" style="width:100%;table-layout:fixed">
		<colgroup>
			<col style="width:2em;" />
			<col style="width:80%;" />
			<col style="width:20%;" />
		</colgroup>
		<tr class="d0"><th>#</th><th>Name</th><th>Album</th></tr>
		<%
		List<TracksDatabase.Track> tracksForArtist = null;
		tracksForArtist = tdb.getTracksByArtist(artistName);
		int i = 0;
		
		String sFormat = "<tr class=\"d%d selectable\" onClick=\"addSong(%d);\"><td>%d</td><td>%s</td><td style=\"border-right:none;\">%s</td></tr>";
		for (TracksDatabase.Track track: tracksForArtist)
			out.println(String.format(sFormat, ++i%2, track.getID(), track.getTrackNumber(), track.getName(), track.getAlbum()));
		%>
	</table>
	
	<script type="text/javascript">
		function addSong(id){
			XMLDataOperation("jukebox.jsp?com=addsong&id=" + id, function(){
				parent.reloadPlayData();
			});
		}
	</script>
</body>

<%}%>
</html>
