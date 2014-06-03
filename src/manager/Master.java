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
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Master implements Runnable{
	
	private ConcurrentHashMap<String, SlaveInfo> slaveTable = null;
	private int port;
	private long slaveNum;
	
	public Master(int port) {
		this.port = port;
		this.slaveTable = new ConcurrentHashMap<String,SlaveInfo>();
		this.slaveNum = 1;
	}
	
	@Override
	public void run() {
			/* Setup server socket (waiting for upcoming slaves) */
			ServerSocket listener = null;
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
				ObjectOutputStream objOutput = null;
				try {
					input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				} catch (IOException e) {
					System.err.println("\tMaster.run():\tIOException happened while creating I/O stream");
				}
				StringBuilder arg = new StringBuilder();
				RequestType val = parseRequest(input, arg);
				
				if (val == RequestType.REGISTER) {
					int listenPort;
					try {
						listenPort = Integer.parseInt(arg.toString());
					} catch (NumberFormatException e) {
						System.err.println("\tMaster.run():\tUnable to recognize the number format.");
						continue;
					}
					if (listenPort < 0 || listenPort > 65535) {
						System.out.println("\tMaster.run():\tReceived invalid listen port number");
						continue;
					}
					StringBuilder rrRst = new StringBuilder();
					RegisterResult rr = registerSlave(socket.getInetAddress(), listenPort, rrRst);
					String info = null;
					if (rr.equals(RegisterResult.SUCCEED)) {
						info = "succeed\t" +  rrRst.toString() + "\n";
					} else if (rr.equals(RegisterResult.REFRESH)) {
						info = String.format("refresh\t%s\n", rrRst.toString());
					} else {
						info = String.format("invalid\n");
					}
					
					try {
						output = new PrintWriter(socket.getOutputStream(), true);
					} catch (IOException e) {
						System.err.println("\tMaster.run():\tIOException happened while instantiating PrintWriter.");
					}
					
					output.write(info);
					output.flush();
					
				} else if (val == RequestType.QUERY) {

					SlaveInfo[] rst = querySlaveInfo(arg.toString());
					try {
						objOutput = new ObjectOutputStream(socket.getOutputStream());
						objOutput.writeObject(rst);
					} catch (IOException e) {
						System.err.println("\tMaster.run():\tIOException happened while output SlaveInfo");
					}					
				} else if (val == RequestType.DEREGISTER) {
					SlaveInfo deregisterNode = this.slaveTable.get(arg.toString());
					String info = null;
					if (deregisterNode == null) {
						String stdInfo = String.format("\tMaster.run():\tDeregistration of Slave node (name:%s) failed.", arg.toString());
						System.out.println(stdInfo);
						info = "failed\n";
					} else {
						deregisterNode.setNodeStatus(NodeStatus.TERMINATED);
						String stdInfo = String.format("\tMaster.run():\tDeregistration of Slave node (name:%s) succeeded.", arg.toString());
						System.out.println(stdInfo);
						info = "succeed\n";
					}
					try {
						output = new PrintWriter(socket.getOutputStream(), true);
					} catch (IOException e) {
						System.err.println("\tMaster.run():\tIOException happened while instantiating PrintWriter.");
					}
					output.write(info);
					output.flush();
					
				}
				
				try {
					if (input != null) {
						input.close();
					}
					if (output != null) {
						output.close();
					}
					if (objOutput != null) {
						objOutput.close();
					}
					if (socket != null) {
						socket.close();
					}
				} catch (IOException e) {
					System.out.println("\tMaster.run():\tIOException occurs while closing I/O.");
				}
			}
	}
	
	private RequestType parseRequest(BufferedReader input, StringBuilder arg) {
		String query;
		try {
			query = input.readLine();
		} catch (IOException e) {
			System.out.println("\t\tMaster.parseRequest():\tError! I/O exception happened in readLine().");
			return RequestType.EXCEPTION;
		}
		
		if (query == null || query.length() < 1) {
			return RequestType.UNIMPLEMENTED;
		}
		
		String[] comp = query.split("\t");
		if (comp.length < 2) {
			return RequestType.UNIMPLEMENTED;
		}
		
		if (comp[0].equals("register")) {
			arg.append(comp[1]);
			return RequestType.REGISTER;
		} else if (comp[0].equals("query")) {
			arg.append(comp[1]);
			return RequestType.QUERY;
		} else if (comp[0].equals("deregister")) {
			arg.append(comp[1]);
			return RequestType.DEREGISTER;
		} else {
			return RequestType.UNIMPLEMENTED;
		}
	}
	
	private RegisterResult registerSlave(InetAddress ip, int port, StringBuilder name) {
		String slaveName = null;
		SlaveInfo info = new SlaveInfo(port, ip);
		Set<String> nodeNameSet = this.slaveTable.keySet();
		for (String nodeName : nodeNameSet) {
			if (this.slaveTable.get(nodeName).getIP().equals(info.getIP()) &&
					this.slaveTable.get(nodeName).getListenPort() == info.getListenPort()) {
				if (slaveTable.get(nodeName).getNodeStatus() == NodeStatus.TERMINATED) {
					System.out.println("\t\tSlave.registerSlave():\tRefresh old slave node:" + slaveName);
					info.setNodeStatus(NodeStatus.RUNNING);
					info.setNodeName(nodeName);
					this.slaveTable.put(nodeName, info);
					name.append(nodeName);
					return RegisterResult.REFRESH;
				} else {
					System.out.println("\t\tSlave.registerSlave():\tReceived an invalid registration");
					return RegisterResult.INVALID;	
				}
			}
		}
		
		slaveName = "slave" + (this.slaveNum);
		this.slaveNum++;
		info.setNodeName(slaveName);
		info.setNodeStatus(NodeStatus.RUNNING);
		slaveTable.put(slaveName, info);
		System.out.println("\t\tSlave.registerSlave():\tRegister a new slave node:" + slaveName);
		name.append(slaveName);
		return RegisterResult.SUCCEED;
	}
	
	private SlaveInfo[] querySlaveInfo(String arg) {
		String querySlaveName = arg;
		SlaveInfo[] response = null;
		if (querySlaveName.equals("all")) {
			Set<String> slaveSet = this.slaveTable.keySet();
			if (slaveSet != null && slaveSet.size() >= 1) {
				response = new SlaveInfo[slaveSet.size()];
				ArrayList<SlaveInfo> list = new ArrayList<SlaveInfo>();
				for (String slaveNode : slaveSet) {
					if (this.slaveTable.get(slaveNode).getNodeStatus() == NodeStatus.RUNNING) {
						list.add(this.slaveTable.get(slaveNode));
					}
				}
				if (list.size() > 0) {
					response = new SlaveInfo[list.size()];
					for (int i = 0; i < list.size(); i++) {
						response[i] = list.get(i);
					}
				}
				
				return response;
			}
		} else {
			if (this.slaveTable.containsKey(querySlaveName) && 
					this.slaveTable.get(querySlaveName).getNodeStatus() == NodeStatus.RUNNING) {
				response = new SlaveInfo[1];
				response[0] = this.slaveTable.get(querySlaveName);
			} 
		}
		return response;

	}

}
