package manager;

import java.net.UnknownHostException;



public class ProcessManager {
	
	public static void main (String[] args) {
//		boolean verbose;
		if (args.length < 3) {
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
		} else {
			Slave slave = null;
			try {
				slave = new Slave("127.0.0.1", 1234);
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
		info += "\tslave mode: <mode> <slave port> <master inet address> <master port> <verbose>\n";
		System.out.println(info);
	}

}
