<%@page contentType="application/json" import="data.DataRequest, raspiejukebox.*, java.io.*, java.util.*"%><%
String dataToGrab = request.getParameter("data");

if (dataToGrab==null)
	out.println("Nothing to grab");
	
else if (dataToGrab.equals("artists"))
	out.print(DataRequest.getArtists());
	
else if (dataToGrab.equals("discography"))
	out.print(DataRequest.getDiscography(request.getParameter("artistname")));
	
else  //Assume they want ALL data
	out.print(DataRequest.getAll());
%>
