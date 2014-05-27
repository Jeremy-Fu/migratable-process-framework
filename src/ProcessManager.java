import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ProcessManager {
	
	public static void main (String[] args) {
		if (args.length < 3) {
			printUsage();
			return;
		}
		
		String mode = args[0];
		if (mode == "master") {
			int port = Integer.parseInt(args[1]);
			
			boolean verbose = false;
			if (args.length > 2) {
				if (args[2] == "true") {
					verbose = true;
				}
			}
			
			ServerSocket socket = null;
			try {
				socket = new ServerSocket(port);
			} catch (IOException e) {
				if (verbose) {
					e.printStackTrace();
					System.out.println("\tmain:\tError! Initialization of socket in master mode failed.");
				}
				return;
			}
			
			while (true) {
				Socket slaveSocket = null;
				try {
					slaveSocket = socket.accept();
				} catch (IOException e) {
					if (verbose) {
						e.printStackTrace();
						System.out.println("\tmain:\tException! Cannot accept a new slave.");
					}
				}
				
			}
			
		}
		
	}
	
	private static void printUsage() {
		String info = "Initialization failed.\n";
		info += "\tmaster mode: <mode> <listen port> <verbose>\n";
		info += "\tslave mode: <mode> <slave port> <master inet address> <master port> <verbose>\n";
		System.out.println(info);
	}

}
