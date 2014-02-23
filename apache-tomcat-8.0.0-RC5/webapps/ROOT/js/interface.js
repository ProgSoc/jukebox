//This is the mutch more specific and technical side
//This makes all the server calls

var getDiscography, getArtists, updateQueue, updateState, updateDatabase, setVolume,
	play, pause, skip, queueSong, ws;
	
(function(){
	var database = null;
	var artists = [];
	
	getDiscography = function(artist){
		if (artist in database)
			return database[artist];
		else
			return [];
	}
	
	getArtists = function(){
		return artists;
	}
	
	function setDatabase(data){
		database = data;
			artists = [];
			for (a in database)
				artists.push(a);
			
			showArtists();
	}
	
	//No Websockets
	function noWebsockets(){
		console.log("Using AJAX");
		
		updateDatabase = function(){
			$.getJSON('getjson.jsp?data=all', setDatabase);
		}
		
		updateQueue = function(){
			$.getJSON('getjson.jsp?data=queue', function(queue){
				displayQueue(queue);
			});
		}
		
		updateState = function(){
			$.getJSON('getjson.jsp?data=state', function(state){
				displayState(state);
			});
		}
		
		updateDatabase();
		updateQueue();
		updateState();
		
		setInterval(function(){
			updateQueue();
			updateState();
		}, 5000);
		
		setInterval(updateDatabase, 300000); //Every 5 minutes
		
		function command(com){
			$.getJSON('jukebox.jsp?com=' + com, function(data){
				updateQueue();
				updateState();
			});
		}
		
		play = function(){command('play');};
		pause = function(){command('pause');};
		skip = function(){command('next');};
		queueSong = function(id){command('queuesong&id=' + id);};
		setVolume = function(level){command('vol&volLevel=' + encodeURI(level));};
	}
	
	if (WebSocket){//DEBUG: IGNORE WEBSOCKETS
		console.log("Using WebSocket");
		var MAX_ATTEMPTS = 10;
		
		function setWebsockets(attempts){
			ws = new WebSocket(document.URL.replace("http", "ws") + 'data/socket');
			var attemptsTimeout = -1;
			
			updateQueue = function(){ws.send('{"id":"queue"}');};
			updateState = function(){ws.send('{"id":"state"}');};
			updateDatabase = function(){ws.send('{"id":"all"}');};
			play = function(){ws.send('{"id":"play"}');};
			pause = function(){ws.send('{"id":"pause"}');};
			skip = function(){ws.send('{"id":"next"}');};
			queueSong = function(id){ws.send('{"id":"queuesong","trackId":' + id + '}');};
			setVolume = function(level){
				var s = '{"id":"vol","volLevel":' + level;
				if (level % 1 == 0)
					s += ".0";
				
				ws.send(s + "}");
			};
			
			//On a successful connection sustained for 5 minutes, reset the amount of attempts
			ws.onopen = function(){
				console.log("Socket opened");
				
				attemptsTimeout = setTimeout(function(){
					
					attempts = MAX_ATTEMPTS;
					attemptsTimeout = -1;
				}, 5*60*1000);
				
				updateState()
				updateDatabase();
				updateQueue();
			};
			
			ws.onclose = ws.onerror = function(){
				console.log("Socket closed");
				
				if (attemptsTimeout !== -1){
					clearInterval(attemptsTimeout);
					attemptsTimeout = -1;
				}
						
				//Retry connect
				if (--attempts > 0)
					setWebsockets(attempts);
				else
					noWebsockets();
			}
			
			
			ws.onmessage = function(data){
				data = $.parseJSON(data.data);
				
				if (data.dataType == 'state')
					displayState(data.data);
				else if (data.dataType == 'queue')
					displayQueue(data.data);
				else if (data.dataType == 'all')
					setDatabase(data.data);
				else if (data.dataType = "volume")
					volumeSlider.setValue(data.data);
					
				else if (data.dataType == 'discography')
					database[data.artistName] = data.data;
			}
			
			
		}
		
		setWebsockets(MAX_ATTEMPTS);
	} else
		noWebsockets();
})();
