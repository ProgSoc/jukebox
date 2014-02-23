<%@ page contentType="text/html" import="raspiejukebox.*, java.io.*" %>

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

<html>
<head>
	<title>Admin Panel</title>

<style type="text/css">
.button {
	margin: 5px;
}
</style>
</head>

<body>
<h1>Basic Admin Panel </h1>

<%
String com = request.getParameter("com");
if (com == null || com.equals("vol")){
	if (com!= null && com.equals("vol")){
		String vol = request.getParameter("volLevel");
		if (!vol.endsWith("%"))
			vol += "%";
		if (vol.matches("([0-9]{1,2})\\%?")){
			Runtime.getRuntime().exec("amixer cset numid=1 -- " + vol);
		}
	}
%>
	<h2>This really is as simple and as lazy as I can possibly make it</h2>
	<h3>Click shit to do shit</h3>
	
	<b>Track Count: </b> <%=TracksDatabase.get().getSize()%><br />
	<div class="button"><a href="admin.jsp?com=refreshdb">Refresh the database</a></div>
	<br />
	<div class="button"><a href="admin.jsp?com=reboot">Reboot the PI</a></div>
	
	<form action="admin.jsp?com=vol" method="POST">
		Volume: <input type="text" name="volLevel" value="<%=getVolume()%>"/>
		<input type="submit" value="Set Vol" />
	</form>
	
<%
} else if (com.equals("refreshdb")){
	TracksDatabase.get().reloadDatabase(true);
	out.println("<h1>REFRESHING</h1>");
} else if (com.equals("reboot")){
	Runtime.getRuntime().exec("sudo reboot");
	out.println("<h1>Rebooting</h1>");
}%>
</body>
</html>
