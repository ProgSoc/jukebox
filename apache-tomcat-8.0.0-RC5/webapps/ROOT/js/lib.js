/*Syntax of js database
Jukebox.db = {
	artistName [
			0: {
				id, name, num, album, artist
			}
			1: {...}
			2: {...}
		]
	},
	artistName {...
	}
}
*/
var volumeSlider = null;

$(document).ready(function(){
	var volumeTimeout = -1;
	volumeSlider = new Slider($('#volume canvas')[0], {
		"minVal": 0,
		"maxVal": 1,
		'onValueChange': function(){
			if (volumeTimeout == -1)
				volumeTimeout = setTimeout(function(){
					setVolume(volumeSlider.value);
					volumeTimeout = -1;
				}, 50);
		},
		"bgColor": "#777777",
		"strokeColor": "black",
		"sliderColor": "lime"
	});
	
	$('#playbutton').on('click', function(e){
		$('#playbutton').hide();
		$('#pausebutton').show();
		
		play();
	});
	
	$('#pausebutton').on('click', function(e){
		$('#pausebutton').hide();
		$('#playbutton').show();
		
		pause();
	});
	
	$('#skipbutton').on('click', function(e){
		skip();	
	});
	
	$('#tracks #backbutton').on('click', function(e){
		showArtists();	
	}).hide();
	showArtists();
})

function displayState(newState){
	$('#playingtrack').html(newState.track.name);
	$('#playingalbum').html(newState.track.album);
	$('#playingartist').html(newState.track.artist);
	
	if (newState.paused){
		$('#playbutton').show();
		$('#pausebutton').hide();
	} else {
		$('#pausebutton').show();
		$('#playbutton').hide();
	}
	
	volumeSlider.setValue(newState.volume)
}

function displayQueue(queue){
	var table = $('<table>')
		.append($('<tr>')
			.append($('<th>').html('#'))
			.append($('<th>').html('Name'))
			.append($('<th>').html('Artist')));
	
	for (var j=0; j<queue.length; j++){
		var row = $('<tr>')
			.append($('<td>').html(j+1))
			.append($('<td>').html(queue[j].name))
			.append($('<td>').html(queue[j].artist));
		
		if (j%2 == 0)
			row.addClass('second');
		
		table.append(row);
	}
	
	$('#queue table').replaceWith(table);
}

var artistsTable = null, artistsScrollTop = 0;

function hideArtists(){
	artistsScrollTop = $('#selections').scrollTop();
	$('#selections table').remove();
	$('#tracks #backbutton').show();
}

function showArtists(){
	$('#tracks h1').html('Artists');
	$('#selections').html(tabulateArtists(getArtists()))
		.scrollTop(artistsScrollTop);
	$('#tracks #backbutton').hide();
}

function tabulateArtists(artists){
	var table = $('<table>')
	
	for (var j=0; j<artists.length; j++){
		var row = $('<tr>')
			.append($('<td>').html(artists[j]))
			.data('artist', artists[j]);
		
		if (j%2 == 0)
			row.addClass('second');
		
		table.append(row);
	}
	
	table.find('tr').on('click', function(e){
		displayDiscography($(this).data('artist'));	
	});
	
	return table;
}

function displayDiscography(artist){
	$('#tracks h1').html(artist);
	hideArtists();
	
	var table = $('<table>')
		.addClass('discography')
	
	var tracks = getDiscography(artist);
	for (var j=0; j<tracks.length; j++){
		var row = $('<tr>')
			.data('id', tracks[j].id)
			.append($('<td>').html(tracks[j].name))
			.append($('<td>').html(tracks[j].album))
		
		if (j%2 == 0)
			row.addClass('second');
		
		table.append(row);
	}
	
	table.find('tr').on('click', function(e){
		queueSong($(this).data('id'));		
	});
	
	table.prepend($('<tr>')
			.addClass('header')
			.append($('<th>').html('Name'))
			.append($('<th>').html('Album')));
	
	$('#selections').html(table);
}
