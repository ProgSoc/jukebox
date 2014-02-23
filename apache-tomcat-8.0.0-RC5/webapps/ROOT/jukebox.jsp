<%@page contentType="application/json" import="raspiejukebox.*, java.io.*, java.util.*"%><%
String success = "Command Successful";
TracksDatabase tdb = TracksDatabase.get();
JukeBox jb = JukeBox.get();


String command = request.getParameter("com");

if (command == null);

else if (command.equals("play"))
	jb.play();
else if (command.equals("pause"))
	jb.pause();
else if (command.equals("next"))
	jb.playNext();
else if (command.equals("queuesong"))
	jb.addToQueue(tdb.getTrackByID(Integer.parseInt(request.getParameter("id"))));
else if (command.equals("refreshdb"))
	tdb.reloadDatabase(true);
else if (command.equals("vol")){
	jb.setVolume(Float.parseFloat(request.getParameter("volLevel")));
}
else
	success = "Command Not Understood";
	
out.print("{\"result\":\"");
out.print(success);
out.print("\"}");
%>
