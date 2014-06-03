package manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
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
					System.out.println(socket.getInetAddress().toString() + "::" + socket.getPort());
				} catch (IOException e) {
					System.out.println("\tMaster.run():\tError! Cannot accept a new connection.");
					continue;
				}
				System.out.println("\tMaster.run():\tAccept a new connection");
				BufferedReader input = null;
				PrintWriter output = null;
				ObjectOutputStream objOutput = null;
				try {
					input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					output = new PrintWriter(socket.getOutputStream(), true);
					objOutput = new ObjectOutputStream(socket.getOutputStream());
				} catch (IOException e) {
					System.out.println("\tMaster.run():\tError! I/O exception happened in creating In/Output stream");
					continue;
				}
				
				JSONObject request = parseRequest(input, output);
				RequestType val = (RequestType)request.get("type");
				if (val == RequestType.REGISTER) {
					registerSlave(request, socket.getInetAddress(), output);
				} else if (val == RequestType.QUERY) {
					try {
						responseSlaveInfo(request, objOutput);
					} catch (IOException e) {
						System.out.println("\tMaster.run():\tError! I/O exception happend in responsing QUEYR.");
					}
				}
				
				try {
					input.close();
					output.close();
					objOutput.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}
	
	private JSONObject parseRequest(BufferedReader input, PrintWriter output) {
		JSONObject rst = new JSONObject();
		String query;
		try {
			query = input.readLine();
		} catch (IOException e) {
			System.out.println("\tMaster.run():\tError! I/O exception happened in readLine().");
			output.write("exception" + "\n");
			output.flush();
			rst.put("type", RequestType.EXCEPTION);
			return rst;
		}
		if (query == null || query.length() < 1) {
			rst.put("type", RequestType.UNIMPLEMENTED);
			return rst;
		}
		String[] comp = query.split("\t");
		if (comp.length < 2) {
			String response = "Format error";
			output.write(response + "\n");
			output.flush();
			rst.put("type", RequestType.UNIMPLEMENTED);
			System.out.println("\tMaster.run():\t" + response);
			return rst;
		}
		if (comp[0].equals("register")) {
			rst.put("type", RequestType.REGISTER);
			rst.put("listenPort", comp[1]);
			return rst;
		} else if (comp[0].equals("query")) {
			rst.put("type", RequestType.QUERY);
			rst.put("slaveName", comp[1]);
			return rst;
		}
		rst.put("type", RequestType.UNIMPLEMENTED);
		return rst;
	}
	
	private void registerSlave(JSONObject request, InetAddress ip, PrintWriter output) {
		String slaveName = "slave" + (slaveInfo.size() + 1);
		JSONObject info = new JSONObject();
		info.put("listenPort", (String)request.get("listenPort"));
		info.put("IP", ip);
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
	
	private void responseSlaveInfo(JSONObject request, ObjectOutputStream output) throws IOException {
		String querySlaveName = (String)request.get("slaveName");
		JSONObject response = new JSONObject();
		if (querySlaveName.equals("all")) {
			Set<String> slaveSet = this.slaveInfo.keySet();
			if (slaveSet == null || slaveSet.size() < 1) {
				response.put("size", 0);
			} else {
				response.put("size", slaveSet.size());
				int i = 0;
				for (String slaveNode : slaveSet) {
					response.put(i, slaveNode);
					i++;
				}
			}
		} else {
			if (this.slaveInfo.containsKey(querySlaveName)) {
				response.put("size", 1);
				response.put(0, this.slaveInfo.get(querySlaveName));
			} else {
				response.put("size", 0);
			}
		}
		
		output.writeObject(response);
		//output.writeChar('\n');
		output.flush();
	}

}
