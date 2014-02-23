package startup;

import raspiejukebox.*;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class InitializeServletListener implements ServletContextListener {
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("\n***************************\n*****JukeBox Destroyed*****\n***************************\n");
		
		//Nothing to do here
	}
 
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("\n***************************\n******JukeBox Created******\n***************************\n");
		
		//Initialize everythin
		TracksDatabase.get(); 
		JukeBox.get();
		
		HardwareInterface.initialise();
	}
}
