package manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Slave implements Runnable{
	private int remotePort = 20024; //port number ranges from 0 ~ 65535
	private InetAddress remoteInetAddress = null;
	private String slaveName = null;
	
	public Slave(String dstIP, int port) throws UnknownHostException {
		this.remotePort = port;
		this.remoteInetAddress = InetAddress.getByName(dstIP);
		this.slaveName = "";
	}
	
	@Override
	public void run() { 
		Socket slaveSocket = null;
		try {
			slaveSocket = new Socket(this.remoteInetAddress, this.remotePort);
			BufferedReader input =
	            new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()));
			String slaveName = input.readLine();
			System.out.println("\tSlave.run():\tReceived new name:" + slaveName);
			this.slaveName = slaveName;
			
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tI/O excpetion in creating a slave socket");
			return;
		}
		
		
	}

}
