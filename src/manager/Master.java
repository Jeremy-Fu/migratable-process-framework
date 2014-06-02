package manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;

public class Master implements Runnable{
	
	private ConcurrentHashMap<String, JSONObject> slaveInfo = null;
	private int port = 0;
	private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
	
	public Master() {
		this.port = 22045;
		this.slaveInfo = new ConcurrentHashMap<String,JSONObject>();
	}
	
	public Master(int port) {
		this.port = port;
		this.slaveInfo = new ConcurrentHashMap<String,JSONObject>();
	}
	
	@Override
	public void run() {
			/* Setup server socket (waiting for upcoming slaves) */
			ServerSocket listener;
			try {
				listener = new ServerSocket();
				listener.bind(new InetSocketAddress(this.port));
			} catch (IOException e) {
				System.out.println("\tMaster.run():\tError! The port is currently used.");
				return;
			}
		
			System.out.println("\tMaster.run():\tMaster server is listening at port:" + this.port);
			
			while (true) {
				System.out.println("\tMaster.run():\tWait...");
				Socket socket;
				try {
					socket = listener.accept();
				} catch (IOException e) {
					System.out.println("\tMaster.run():\tError! Cannot accept a new connection.");
					continue;
				}
				System.out.println("\tMaster.run():\tAccept a new connection");
				BufferedReader input = null;
				PrintWriter output = null;
				try {
					input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					output = new PrintWriter(socket.getOutputStream(), true);
				} catch (IOException e) {
					System.out.println("\tMaster.run():\tError! I/O exception happened in creating In/Output stream");
					continue;
				}
				
				String query;
				try {
					query = input.readLine();
				} catch (IOException e) {
					System.out.println("\tMaster.run():\tError! I/O exception happened in readLine().");
					continue;
				}
				
				String[] comp = query.split("\t");
				if (comp.length < 2) {
					String response = "Format error!";
					output.write(response);
					System.out.println("\tMaster.run():\t" + response);
					continue;
				}
				
				if (comp[0].equals("register")) {
					String slaveName = "slave" + (slaveInfo.size() + 1);
					JSONObject info = new JSONObject();
					info.put("listenPort", comp[1]);
					info.put("IP", socket.getInetAddress());
					if (slaveInfo.containsValue(info)) {
						Set<String> nodeSet = slaveInfo.keySet();
						for (String nodeName : nodeSet) {
							if (slaveInfo.get(nodeName).equals(info)) {
								slaveName = nodeName;
								break;
							}
						}
						output.write("refresh\t" + slaveName + "\n");
						System.out.println("ProcessManager.main():\tRefresh old slave node:" + slaveName);
					} else {
						slaveInfo.put(slaveName, info);
						output.write("succeed\t" + slaveName + "\n");
						System.out.println("ProcessManager.main():\tRegister a new slave node:" + slaveName);
					}
					output.flush();
					
				}
			}
	}

}
