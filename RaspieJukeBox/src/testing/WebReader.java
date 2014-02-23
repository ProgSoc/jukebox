package testing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;


public class WebReader {
 
    public static String getPage(String url) {
        try {
            URL google = new URL(url);
            URLConnection yc = google.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc
                    .getInputStream()));
            String inputLine, page="";
            while ((inputLine = in.readLine()) != null)
            	page += inputLine;
            
            in.close();
            return page;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
 
}