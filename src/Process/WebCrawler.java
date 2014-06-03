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
	private volatile boolean suspending = false;
	private volatile boolean finished = false;
	private int maxLine;
	TransactionalFileOutputStream output;
	private int currentLine;
	
	public WebCrawler(String[] args) throws Exception {
		if (args == null || args.length != 3) {
			System.out.println("usage:\tWebCrawler <init url> <output file> <max lines>");
			throw new Exception("Invalid arguments");
		}
		
		this.initURL = args[0];
		this.urlQueue = new LinkedList<String>();
		this.output = new TransactionalFileOutputStream(args[1], "rw");
		try {
			this.maxLine = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			System.err.println("\t\tWebCrawler():\tInvalid number format of <max lines>.");
		}
		this.urlQueue.offer(new String(this.initURL));
		this.verbose = false;
		this.currentLine = 0;
	}
	
	public void run() {
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
			
			this.currentLine++;
			finished = (this.urlQueue.isEmpty()) || (this.currentLine >= this.maxLine);
			
			try {
				Thread.sleep(1000); //Sleep for 1 second.
			} catch (InterruptedException e) {
				System.out.println("\t\tWebCrawler.run():\tInterruptedException caught while thread is sleeping.");
			}
		}
		
		if (finished) {
			try {
				output.close();
			} catch (IOException e) {
				if (this.verbose) {
					System.out.println("\t\tWebCrawler.run():\tIOException occured while closing output stream.");
				}
			}
		}
		suspending  = false;
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
		this.suspending = true;
		int i = 0;
		while (this.suspending && !this.finished) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				System.err.println("\t\tWebCrawler.suspend():\tInterruption is called ");
			}
		}
		
		
	}

	
	@Override
	public int getStatus() {
		if (this.finished) {
			return 0;
		} else if (this.suspending) {
			return -1;
		} else {
			return 1;
		}
	}
}


/* REFERENCE */
//[1]. http://stackoverflow.com/questions/163360/regular-expresion-to-match-urls-in-java