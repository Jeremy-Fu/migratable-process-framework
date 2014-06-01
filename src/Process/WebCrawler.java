package process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import transactionalIO.TransactionalFileOutputStream;

public class WebCrawler {
	private final String USER_AGENT = "Mozilla/5.0";
	private final String HTTP_REGEX = "http://(\\w+\\.)+(\\w+)";
	private static String INIT_URL = "http://www.google.com/search?q=geng";
	//HTTP regular expression, [1]
	private final Pattern HTTP_PATTERN = Pattern.compile(HTTP_REGEX);
	private Queue<String> urlQueue = new LinkedList<String>();
	private boolean verbose = false;
	
	
	public static void main (String[] args) {
		WebCrawler webcrawler = new WebCrawler();
		webcrawler.urlQueue.offer(new String(INIT_URL));
		while (!webcrawler.urlQueue.isEmpty()) {
			String url = webcrawler.urlQueue.poll();
			if (webcrawler.verbose) {
				System.out.println("Request page:\t" + url);
			}
			try {
				ArrayList<String> tmp = webcrawler.sendGetRequest(url);
				if (tmp != null) {
					for (String ele : tmp) {
						webcrawler.urlQueue.offer(ele);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		
	}
	
	private ArrayList<String> sendGetRequest(String url)
		throws Exception {
		ArrayList<String> rst = new ArrayList<String>();
		URL urlObject = null;
		HttpURLConnection con = null;
		BufferedReader in = null;
		try {
			urlObject = new URL(url);
			con = (HttpURLConnection) urlObject.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", USER_AGENT);
			in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			TransactionalFileOutputStream file = new TransactionalFileOutputStream("output_test.txt","rw");
			String inputLine;
			StringBuffer response = new StringBuffer();
			
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			Matcher ma = HTTP_PATTERN.matcher(response.toString());
			while (ma.find()) {
				rst.add(ma.group());
				if (this.verbose) {
					System.out.println("\t\t\tadd:\t" + ma.group());
				}
				int i = 0;
				while(i < ma.group().length()) {
					file.write(ma.group().charAt(i));
					i++;
				}
				file.write('\n');
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return rst;
	}
}


/* REFERENCE */
//[1]. http://stackoverflow.com/questions/163360/regular-expresion-to-match-urls-in-java