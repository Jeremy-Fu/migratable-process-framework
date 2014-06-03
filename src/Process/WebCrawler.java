package process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import transactionalIO.TransactionalFileOutputStream;

public class WebCrawler implements MigratableProcess{
	private final String USER_AGENT = "Mozilla/5.0";
	private final String HTTP_REGEX = "http://(\\w+\\.)+(\\w+)";
	private final Pattern HTTP_PATTERN = Pattern.compile(HTTP_REGEX);
	//HTTP regular expression, [1]
	private static String initURL;
	private Queue<String> urlQueue;
	private boolean verbose = false;
	private boolean suspending = false;
	private boolean finished = false;
	TransactionalFileOutputStream output;
	
	public WebCrawler(String[] args) throws Exception {
		if (args == null || args.length != 2) {
			System.out.println("usage:\tWebCrawler <init url> <output file>");
			throw new Exception("Invalid arguments");
		}
		
		this.initURL = args[0];
		this.urlQueue = new LinkedList<String>();
		this.output = new TransactionalFileOutputStream(args[1], "rw");
		this.verbose = false;
	}
	
	public void run() {
		this.urlQueue.offer(new String(this.initURL));
		
		while (!finished && !suspending) {
			String url = this.urlQueue.poll();
			try {
				output.write((url + "\n").getBytes());
			} catch (IOException e) {
				if (this.verbose) {
					System.out.println("\t\tWebCrawler.sendGETRequest():\tIOException occured while writing to file.");
				}
			}
			if (this.verbose) {
				System.out.println("\t\tWebCrawler.run():\tRequest page:\t" + url);
			}
			
			ArrayList<String> tmp = this.sendGetRequest(url);
			if (tmp == null) {
				continue;
			}
				
			for (String ele : tmp) {
					this.urlQueue.offer(ele);
			}
			
			
			finished = this.urlQueue.isEmpty();
			try {
				Thread.sleep(1000); //Sleep for 1 second.
			} catch (InterruptedException e) {
				System.out.println("\t\tWebCrawler.run():\tInterruptedException caught while thread is sleeping.");
			}
		}
		
		finished = this.urlQueue.isEmpty();
		if (!finished) {
			suspending = false;
		} else {
			try {
				output.close();
			} catch (IOException e) {
				if (this.verbose) {
					System.out.println("\t\tWebCrawler.run():\tIOException occured while closing output stream.");
				}
			}
		}
		return;
	}

	
	private ArrayList<String> sendGetRequest(String url) {
		ArrayList<String> rst = new ArrayList<String>();
		HttpURLConnection con = null;
		BufferedReader in = null;
		
		URL urlObject = null;
		try {
			urlObject = new URL(url);
		} catch (MalformedURLException e) {
			if (this.verbose) {
				System.out.println("\t\tWebCrawler.sendGETRequest():\tMalformed URL.");
			}
			return null;
		}
		
		try {
			con = (HttpURLConnection) urlObject.openConnection();
		} catch (IOException e) {
			if (this.verbose) {
				System.out.println("\t\tWebCrawler.sendGETRequest():\tIOException occured while open HttpURLConnection.");
			}
			return null;
		}
		
		try {
			con.setRequestMethod("GET");
		} catch (ProtocolException e) {
			if (this.verbose) {
				System.out.println("\t\tWebCrawler.sendGETRequest():\tProtocolException occured while set HTTP request method.");
			}
			return null;
		}
		con.setRequestProperty("User-Agent", USER_AGENT);
		try {
			in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
		} catch (IOException e) {
			if (this.verbose) {
				System.out.println("\t\tWebCrawler.sendGETRequest():\tIOException occured while instantiating BufferedReader.");
			}
			return null;
		}
		
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		try {
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		} catch (IOException e) {
			if (this.verbose) {
				System.out.println("\t\tWebCrawler.sendGETRequest():\tIOException occured while reading a line");
			}
			return null;
		}
		
		Matcher ma = HTTP_PATTERN.matcher(response.toString());
		while (ma.find()) {
			rst.add(ma.group());
			if (this.verbose) {
				System.out.println("\t\tWebCrawler.sendGETRequest():add\t" + ma.group());
			}
		
		}
		
		try {
			in.close();
		} catch (IOException e) {
			if (this.verbose) {
				System.out.println("\t\tWebCrawler.sendGETRequest():\tIOException occured while closing input stream.");
			}
		}
		return rst;
	}

	@Override
	public void suspend() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getFinished() {
		// TODO Auto-generated method stub
		return false;
	}
}


/* REFERENCE */
//[1]. http://stackoverflow.com/questions/163360/regular-expresion-to-match-urls-in-java