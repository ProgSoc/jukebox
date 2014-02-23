//All of mah java script

function XMLDataOperation(url, f) {//callbackfn:fn you need to load after xml loads, args:argument array to pass to the call back function
	xmlhttp = new XMLHttpRequest();
	xmlhttp.overrideMimeType("text/xml");
	xmlhttp.onreadystatechange = function(){
		if (xmlhttp.readyState == 4)
			f(xmlhttp.responseXML);
	}
	xmlhttp.open("GET", url, true);
	xmlhttp.setRequestHeader("Content-Type", "text/xml");
	xmlhttp.send();	
}

