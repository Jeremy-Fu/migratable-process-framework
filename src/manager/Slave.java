package manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;

import process.MigratableProcess;

public class Slave implements Runnable{
	private int remotePort = 20024; //port number ranges from 0 ~ 65535
	private int listenPort = 12345; 
	private InetAddress remoteInetAddress = null;
	private String slaveName = null;
	private ConcurrentHashMap<String, MigratableProcess> processTable = null;
	private long processCounter;
	
	public Slave(String dstIP, int remotePort, int listenPort) throws UnknownHostException {
		this.remotePort = remotePort;
		this.listenPort = listenPort;
		this.remoteInetAddress = InetAddress.getByName(dstIP);
		this.slaveName = "";
		this.processTable = new ConcurrentHashMap<String, MigratableProcess>();
		processCounter = 0;
	}
	
	@Override
	public void run() { 
		/* Register on master */
		try {
			Socket slaveSocket = null;
			BufferedReader input = null;
			PrintWriter output = null;
			
			slaveSocket = new Socket(this.remoteInetAddress, this.remotePort);
			input =
	            new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()));
			output = 
					new PrintWriter(slaveSocket.getOutputStream(), true);
			System.out.println("\tSlave.run():\tTry to register on slave.");
			output.write("register\t" + this.listenPort + "\n");
			output.flush();
			String slaveName = input.readLine().split("\t")[1];
			System.out.println("\tSlave.run():\tReceived new name:" + slaveName);
			this.slaveName = slaveName;
			
			input.close();
			output.close();
			slaveSocket.close();
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOExcpetion occurs in creating a slave socket");
			return;
		}
		
		MigrationHandler mh = new MigrationHandler(this.listenPort);
		Thread mhThread = new Thread(mh);
		mhThread.start();
		
		/* Listen to users command and migration request */
		while (true) {
			BufferedReader stdinInput = 
					new BufferedReader(new InputStreamReader(System.in));
			System.out.print(">>");
			String stdInput = null;
			try{
				stdInput = stdinInput.readLine();
			} catch (IOException e) {
				System.out.println("\tSlave.run():\tIOException occurs while waiting for a stdin input");
				continue;
			}
			
			if (stdInput == null || stdInput.length() < 1) {
				continue;
			}
			String[] comp = stdInput.split(" ");
			if (comp[0].equals("query")) {
				nodesQueryHanlder(comp);
				continue;
			}
			
			if (comp[0].equals("run")) {
				runProcess(comp);
				continue;
			}
			
			if (comp[0].equals("migrate")) {
				migrateProcess(comp);
				continue;
			}
			
			if (comp[0].equals("ps")) {
				System.out.println("\tSlave.run():\tPID\tStatus");
				Set<String> procSet = this.processTable.keySet();
				for (String procName : procSet) {
					MigratableProcess proc = this.processTable.get(procName);
					int procStatCode = proc.getStatus();
					String procStat = null;
					if (procStatCode == 0) {
						procStat = "Finished";
					} else if (procStatCode == -1){
						procStat = "Suspending";
					} else {
						procStat = "Running";
					}
					String info = String.format("\t\t\t%s\t%s", procName, procStat);
					System.out.println(info);
				}
			}
			
			if (comp[0].equals("quit")) {
				System.out.println("\tSlave.run():\tClosing slave server...");
				mh.kill();
				System.out.println("\tSlave.run():\tMigrationHandler terminated.");
				
				Set<String> procSet = this.processTable.keySet();
				for (String procName : procSet) {
					MigratableProcess proc = this.processTable.get(procName);
					proc.suspend();
					System.out.println("\tSlave.run():\tTerminated process:" + procName);
				}
				try {
					stdinInput.close();
				} catch (IOException e) {
					System.out.println("\tSlave.run():\tIOException occurs while Closing stdin input stream.");
				}
				break;
			}	
		}	
	}
	
	private void migrateProcess(String[] args) {
		if (args.length != 3) {
			System.out.println("\tSlave.run():\tMigration usage: migrate <PID> <Slave ID>");
			return;
		}
		String pid = args[1];
		String dstNode = args[2];
		Socket commSocket = null;
		try {
			commSocket = 
				new Socket(this.remoteInetAddress, this.remotePort);
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while instantiating Socket.");
		}
		if (commSocket == null) {
			return;
		}
		ObjectInputStream socketInput = null;
		try {
			socketInput = new ObjectInputStream(commSocket.getInputStream());
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while instantiating ObjectInputStream of commSocket.");
		}
		PrintWriter socketOutput = null;
		try {
			socketOutput = new PrintWriter(commSocket.getOutputStream(), true);
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while instantiating PrintWriter of commSocket.");
		}
		if (socketInput == null || socketOutput == null) {
			return;
		}
		String query = String.format("query\t%s\n",dstNode);
		socketOutput.write(query);
		socketOutput.flush();
		/* Receive response */
		JSONObject node = null;
		try {
			node = (JSONObject)socketInput.readObject();
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while receiving messages of query");
		} catch (ClassNotFoundException e) {
			System.out.println("\tSlave.run():\tClassNotFoundException occrus while reconstructing JSONObject.");
		}
		
		if (node == null) {
			return;
		}
		
		
		try {
			socketInput.close();
			socketOutput.close();
			commSocket.close();
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while closing IO and socket.");
		} finally {
			if (socketOutput != null) {
				socketOutput.close();
			}
		}
		
		JSONObject slaveInfo = (JSONObject)node.get(0);
		InetAddress dstNodeIP = (InetAddress)slaveInfo.get("IP");
		int dstNodePort = Integer.parseInt((String)slaveInfo.get("listenPort"));
		
		try {
			commSocket = 
				new Socket(dstNodeIP, dstNodePort);
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while instantiating Socket.");
		}
		
		ObjectOutputStream objOutput = null;
		try {
			objOutput = new ObjectOutputStream(commSocket.getOutputStream());
		
			MigratableProcess migratedProc = this.processTable.get(pid);
			migratedProc.suspend();
			objOutput.writeObject(migratedProc);
			objOutput.close();
			this.processTable.remove(pid);
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		return;
	}
	
	private boolean nodesQueryHanlder(String[] args) {
		Socket commSocket = null;
		try {
			commSocket = 
				new Socket(this.remoteInetAddress, this.remotePort);
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while instantiating Socket.");
		}
		
		if (commSocket == null) {
			return false;
		}
		
		ObjectInputStream socketInput = null;
		try {
			socketInput = new ObjectInputStream(commSocket.getInputStream());
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while instantiating ObjectInputStream of commSocket.");
		}
		PrintWriter socketOutput = null;
		try {
			socketOutput = new PrintWriter(commSocket.getOutputStream(), true);
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while instantiating PrintWriter of commSocket.");
		}
		
		if (socketInput == null || socketOutput == null) {
			return false;
		}
		
		socketOutput.write("query\tall\n");
		socketOutput.flush();
		/* Receive response */
		JSONObject nodes = null;
		try {
			nodes = (JSONObject)socketInput.readObject();
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while receiving messages of query");
		} catch (ClassNotFoundException e) {
			System.out.println("\tSlave.run():\tClassNotFoundException occrus while reconstructing JSONObject.");
		}
		
		if (nodes == null) {
			return false;
		}
		
		if ((Integer)nodes.get("size") < 1) {
			System.out.println("Slave.run():\tNo slaves found");
		} else {
			System.out.println("Slave.run():\tAll slaves:");
			for (Integer i = 0; i < (Integer)nodes.get("size"); i++) {
				System.out.println("\t\t\t" + nodes.get(i));
			}
		}
		try {
			socketInput.close();
			socketOutput.close();
			commSocket.close();
		} catch (IOException e) {
			System.out.println("\tSlave.run():\tIOException occurs while closing IO and socket.");
		} finally {
			if (socketOutput != null) {
				socketOutput.close();
				
			}
		}
		return true;
	}
	
	public class MigrationHandler implements Runnable {
		private int listenPort;
		private volatile boolean running;
		private volatile boolean suspending;
		
		public MigrationHandler(int port) {
			this.listenPort  = port;
			this.running = true;
			this.suspending = false;
		}
		
		private void kill () {
			this.suspending = true;
			while (suspending && running) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		@Override
		public void run() {
			ServerSocket listener = null;
			try {
				listener = new ServerSocket();
				listener.bind(new InetSocketAddress(this.listenPort));
				listener.setSoTimeout(5000);
			} catch (IOException e) {
				System.out.println("\t\tMigrationHandler.run():\tError! The port is currently used.");
				this.running = false;
				return;
			}
			String info = String.format("\t\tMigrationHandler.run():\t%s is "
					+ "listening at port %s for migration purpose.", slaveName, this.listenPort);
			System.out.println(info);
			while (!this.suspending) {
				//System.out.println("\t\tMigrationHandler.run():\tWait...");
				Socket socket = null;
				try {
					socket = listener.accept();
				} catch (IOException e) {
					//System.out.println("\t\tMigrationHandler.run():\tIOException occurs while accpeting a migration.");
					continue;
				}
				
				if (socket == null) {
					continue;
				}
				
				System.out.println("\t\tMigrationHandler.run():\tAccept a new connection");
				BufferedReader input = null;
				ObjectInputStream objInput = null;
				PrintWriter output = null;

				try {
//					input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					output = new PrintWriter(socket.getOutputStream(), true);
					objInput = new ObjectInputStream(socket.getInputStream());
				} catch (IOException e) {
					System.out.println("\t\tMigrationHandler.run():\tIOException happened in creating In/Output stream");
					e.printStackTrace();
					continue;
				}
				
//				String cmd = null;
//				try {
//					cmd = input.readLine();
//				} catch (IOException e) {
//					System.out.println("\t\tMigrationHandler.run():\tIOException happened in reading from input stream");
//					continue;
//				}
//				
//				if (cmd == null) {
//					System.out.println("\t\tMigrationHandler.run():\null is returned in reading from input stream");
//					continue;
//				}
				
//				if (cmd.equals("migrate")) {

				MigratableProcess proc = null;
//				if (objInput == null) {
//					continue;
//				}
					
				try {
					proc = (MigratableProcess)objInput.readObject();
				} catch (IOException e) {
					System.out.println("\t\tMigrationHandler.run():\tIOException happened while reading process.");
					continue;
				} catch (ClassNotFoundException e) {
					System.out.println("\t\tMigrationHandler.run():\tClassNotFoundException happened while reading process.");
					continue;
				}
				if (proc == null) {
					continue;
				}
				String procName = "migrate-" + Slave.this.slaveName + "-" + Slave.this.processCounter;
				Thread newThread = new Thread(proc, procName);
				newThread.start();
				Slave.this.processTable.put(procName, proc);
				
				try {
					socket.close();
//					input.close();
					output.close();
					objInput.close();
				} catch (IOException e) {
					System.out.println("\t\tMigrationHandler.run():IOException happened while cleaning up.");
				}
			}
			
			try {
				listener.close();
			} catch (IOException e) {
				System.out.println("\t\tMigrationHandler.run():IOException happend while closing listener socket.");
			}
			this.suspending = false;
			this.running = false;
		}
		
	}
	
	private void runProcess(String[] args) {
		MigratableProcess migratableProcess = null;
		try {
			Class<MigratableProcess> aClass = (Class<MigratableProcess>) Class.forName(args[1]);
			Constructor<?> aConstructor = aClass.getConstructor(new Class[]{String[].class});
			String[] constructorArgs = new String[args.length - 2];
			for (int i = 0; i < constructorArgs.length; i++) {
				constructorArgs[i] = args[i + 2];
			}
			migratableProcess = (MigratableProcess) aConstructor.newInstance((Object)constructorArgs);
		} catch (ClassNotFoundException e) {
			String info = String.format("\tSlave.runProcess():\tThe requested class(%s) is not found.", args[1]);
			System.out.println(info);
			return;
		} catch (NoSuchMethodException e) {
			System.out.println("\tSlave.runProcess():\tMigratableProcess requires a constructor which takes String[] as parameters.");
			return;
		} catch (SecurityException e) {
			System.out.println("\tSlave.runProcess():\tAccess to the constructor is denied.");
			return;
		} catch (InstantiationException e) {
			System.out.println("\tSlave.runProcess():\tThe Constructor object is inaccessible or the Class is abstract.");
			return;
		} catch (IllegalArgumentException e) {
			System.out.println("\tSlave.runProcess():\tIllegal prarameter is passed.");
			return;
		} catch (InvocationTargetException e) {
			System.out.println("\tSlave.runProcess():\tUnderlying constructor throws an exception.");
			return;
		} catch (ExceptionInInitializerError e) {
			System.out.println("\tSlave.runProcess():\tInitialization of this method failed.");
			return;
		} catch (IllegalAccessException e) {
			System.out.println("\tSlave.runProcess():\tConstructor object is enforcing Java language access control and the underlying constructor is inaccessible.");
			return;
		}
		
		if (migratableProcess == null) {
			return;
		}
		
		String processName = this.slaveName + "-" + this.processCounter;
		this.processCounter++;
		this.processTable.put(processName, migratableProcess);
		Thread newThread = new Thread(migratableProcess);
		newThread.start();
	}

}
	
/* Referecne */
//[1].	http://tutorials.jenkov.com/java-reflection/constructors.html