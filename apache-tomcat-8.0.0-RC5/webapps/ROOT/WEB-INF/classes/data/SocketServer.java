package data;

import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.json.simple.*;

import raspiejukebox.*;

@ServerEndpoint(value = "/data/socket")
public class SocketServer {
	
	static {
		JukeBox jb = JukeBox.get();
		
		jb.addStateChangeListener(new JukeBoxStateListener() {
			public void stateChanged(JukeBoxEvent e){
				broadcast(DataOut.STATE.get(null));
			}
		});
		
		jb.addQueueChangeListener(new JukeBoxQueueListener(){
			public void queueChanged(JukeBoxEvent e){
				broadcast(DataOut.QUEUE.get(null));
			}
		});
	}
	
	public static enum DataOut {
			ARTISTS	("artists"),
			ALL		("all"),
			VOLUME	("volume"),
			QUEUE	("queue"),
			STATE	("state"),
			DISCO	("discography");
			
			public final String id;
			DataOut(String id){
				this.id = id;
			}
			
			
			String format(CharSequence extraHeader, CharSequence data){
				if (extraHeader == null)
					return String.format("{\"dataType\":\"%s\", \"data\":%s}",
						id, data);
				else
					return String.format("{\"dataType\":\"%s\",%s, \"data\":%s}",
						id, extraHeader, data);
			}
			
			String format(CharSequence content){
				return format(null, content);
			}
			
			String get(){
				return get(null);
			}
			
			String get(Map params){
				switch(this){
					case ARTISTS:
						return format(DataRequest.getArtists());
					case ALL:
						return format(DataRequest.getAll());
					case VOLUME:
						return format(Float.toString(DataRequest.getVolume()));
					case STATE:
						return format(DataRequest.getCurrentState());
					case DISCO:
						String artistName = (String)params.get("artistName");
						return format(
							String.format("\"artistName\":\"%s\"", DataRequest.stringify(artistName)), 
							DataRequest.getDiscography(artistName));
					case QUEUE:
						return format(DataRequest.getQueue());
					default:
						return "{\"dataType\":null,\"data\":null}";
				}
			}
	}
	
	private static List<SocketServer> clients = new ArrayList<SocketServer>(5);

	public static void broadcast(String message){
		for (SocketServer c: clients)
			c.say(message);
	}
	
	private Session session;
	
	@OnOpen
	public void start(Session s){
		session = s;
		clients.add(this);
	}
	
	@OnClose
	public void end(){
		clients.remove(this);
	}
	
	@OnMessage
	public void incoming(String data){
		//Parse and determine
		JSONObject parsed = (JSONObject)JSONValue.parse(data);
		String id = (String)parsed.get("id");
		JukeBox jb = JukeBox.get();
		TracksDatabase tdb = TracksDatabase.get();
		
		
		if (id == null);
			//Do Nothing
		else if (id.equals("play"))
			jb.play();
		else if (id.equals("pause"))
			jb.pause();
		else if (id.equals("next"))
			jb.playNext();
		else if (id.equals("queuesong") && parsed.get("trackId") != null)
			jb.addToQueue(tdb.getTrackByID(((Long)parsed.get("trackId")).intValue()));
		else if (id.equals("refreshdb"))
			tdb.reloadDatabase(true);
		else if (id.equals("vol")){
			Object vol = parsed.get("volLevel");
			if (vol != null){
				jb.setVolume((float)((double)vol));
				broadcast(DataOut.VOLUME.get());
			}
		} else
			for (DataOut d: DataOut.values())
				if (d.id.equals(id)){
					say(d.get(parsed));
					break;
				}
	}
	
	public void say(String s){
		try{
			session.getBasicRemote().sendText(s);
		} catch (IOException e){
			//Gawnskies
			try {
				session.close();
			} catch (IOException e2){}
		}
	}
}
