package manager;

import java.net.UnknownHostException;



public class ProcessManager {
	
	public static void main (String[] args) {
//		boolean verbose;
		if (args.length < 2) {
			printUsage();
			return;
		}
		
		String mode = args[0];
		System.out.println("ProcessManager.main():\t mode:" + mode);
		if (mode.equals("master")) {
			int port = 20000;
			try {
				port = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.print("ProcessManager.main():\t Error! The second parameter should be an integer. ");
				System.out.println("Master starts failed.");
				return;
			}
			
			System.out.println("ProcessManager.main():\t Starting the master server...");
			Master master = new Master(port);
			Thread masterThread = new Thread(master);
			masterThread.start();
			try {
				masterThread.join(0);
			} catch (InterruptedException e) {
				System.out.println("ProcessManager.main():\t Warning! Received an interuption.");
				e.printStackTrace();
			}
		} else if (mode.equals("slave")){
			Slave slave = null;
			String remoteInetAddr = args[1];
			int remotePort = 22045;
			try {
				remotePort = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				System.out.println("ProcessManager.main():\tError! Unknown number format of remote port.");
				return;
			}
			if (remotePort < 0 || remotePort > 65535) {
				printPortRange();
				return;
			}
			int listenPort = 0;
			try {
				listenPort = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				System.out.println("ProcessManager.main():\tError! Unknown number format of listen port.");
				return;
			}
			if (listenPort < 0 || listenPort > 65535) {
				printPortRange();
				return;
			}
			
			try {
				slave = new Slave(remoteInetAddr, remotePort, listenPort);
			} catch (UnknownHostException e) {
				System.out.println("ProcessManager.main():\tError! Unknown host.");
				return;
			}
			System.out.println("ProcessManager.main():\tStarting slave server...");
			Thread slaveThread = new Thread(slave);
			slaveThread.start();
			try {
				slaveThread.join(0);
			} catch (InterruptedException e) {
				System.out.println("ProcessManager.main():\t Warning! Received an interuption.");
				e.printStackTrace();
			}
		}
		return;
		
	}
	
	private static void printUsage() {
		String info = "Initialization failed.\n";
		info += "\tmaster mode: <mode> <listen port> <verbose>\n";
		info += "\tslave mode: <mode> <remote inet address> <remote port> <listen port> <verbose>\n";
		System.out.println(info);
	}
	
	private static void printPortRange() {
		System.out.println("ProcessManager.main():\tError! Port number should be 0 ~ 65535.");
	}

}
