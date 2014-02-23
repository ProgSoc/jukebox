<%@ page contentType="text/xml" import="raspiejukebox.*, java.io.*"%>

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

<result>
success

<%
JukeBox jb = JukeBox.get();
TracksDatabase tdb = TracksDatabase.get();

if (!HardwareInterface.isInitialised())
	HardwareInterface.initialise();

String command = request.getParameter("com");

if (command == null);
else if (command.equals("play"))
	jb.play();
else if (command.equals("pause"))
	jb.pause();
else if (command.equals("next"))
	jb.playNext();
else if (command.equals("addsong"))
	jb.addToQueue(tdb.getTrackByID(Integer.parseInt(request.getParameter("id"))));
else if (command.equals("refreshdb"))
	tdb.reloadDatabase(true);
else if (command.equals("vol")){
	String vol = request.getParameter("volLevel");
	if (!vol.endsWith("%"))
		vol += "%";
	if (vol.matches("([0-9]{1,2})\\%?")){
		Runtime.getRuntime().exec("amixer cset numid=1 -- " + vol);
	}

}
else{
	%>Command unknown<%
}
%>

</result>
