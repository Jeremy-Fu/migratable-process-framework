package manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Set;


import org.json.simple.JSONObject;

import process.MigratableProcess;

public class Slave implements Runnable{
	private int remotePort = 20024; //port number ranges from 0 ~ 65535
	private int listenPort = 12345; 
	private InetAddress remoteInetAddress = null;
	private String slaveName = null;
	private Hashtable<String, MigratableProcess> processTable = null;
	private long processCounter;
	
	public Slave(String dstIP, int remotePort, int listenPort) throws UnknownHostException {
		this.remotePort = remotePort;
		this.listenPort = listenPort;
		this.remoteInetAddress = InetAddress.getByName(dstIP);
		this.slaveName = "";
		this.processTable = new Hashtable<String, MigratableProcess>();
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
		
		/* Send query request */
//		if (args.length > 1) {
//			socketOutput.write("query\t" + args[1] + "\n");
//			socketOutput.flush();
//		} else {
//			socketOutput.write("query\tall\n");
//			socketOutput.flush();
//		}
		
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