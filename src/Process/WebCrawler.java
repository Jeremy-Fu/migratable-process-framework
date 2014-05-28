package Process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebCrawler {
	private final String USER_AGENT = "Mozilla/5.0";
	
	public static void main (String[] args) {
		WebCrawler webcrawler = new WebCrawler();
		webcrawler.sendGetRequest();
	}
	
	private void sendGetRequest() {
		String url = "http://www.google.com/search?q=mkyong";
		URL urlObject;
		try {
			urlObject = new URL(url);
			HttpURLConnection con;
			con = (HttpURLConnection) urlObject.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", USER_AGENT);
			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
	 
			//print result
			System.out.println(response.toString());
	 
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
}
