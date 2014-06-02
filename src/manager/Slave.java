package manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Slave implements Runnable{
	private int remotePort = 20024; //port number ranges from 0 ~ 65535
	private int listenPort = 12345; 
	private InetAddress remoteInetAddress = null;
	private String slaveName = null;
	
	public Slave(String dstIP, int remotePort, int listenPort) throws UnknownHostException {
		this.remotePort = remotePort;
		this.listenPort = listenPort;
		this.remoteInetAddress = InetAddress.getByName(dstIP);
		this.slaveName = "";
	}
	
	@Override
	public void run() { 
		/* Register on master */
		Socket slaveSocket = null;
		try {
			slaveSocket = new Socket(this.remoteInetAddress, this.remotePort);
			BufferedReader input =
	            new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()));
			PrintWriter output = 
					new PrintWriter(slaveSocket.getOutputStream(), true);
			System.out.println("\tSlave.run():\tTry to register on slave.");
			output.write("register\t" + this.listenPort + "\n");
			output.flush();
			String slaveName = input.readLine().split("\t")[1];
			System.out.println("\tSlave.run():\tReceived new name:" + slaveName);
			this.slaveName = slaveName;
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tI/O excpetion in creating a slave socket");
			return;
		}
		
		
	}

}
