import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;


public class ProcessManager {
	
	public static void main (String[] args) {
		if (args.length < 3) {
			printUsage();
			return;
		}
		
		String mode = args[0];
		if (mode == "master") {
			int port = Integer.parseInt(args[1]);
			
			if (args.length > 2) {
				if (args[2] == "true") {
					verbose = true;
				}
			}
			try {
				/* Setup server socket (waiting for upcoming slaves) */
				ServerSocketChannel serverChannel = ServerSocketChannel.open();
				ServerSocket serverSocket = serverChannel.socket();
				serverSocket.bind(new InetSocketAddress(port));
				
				/* Setup multiplexed I/O (waiting for both upcoming slaves and stdin) */
				Selector selector  = Selector.open();
				
				/* Register server channel with selector */
				serverChannel.configureBlocking(false);
				serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			
				while (true) {
					int n = selector.select();
					
					if (n == 0) {
						continue;
					}
					
					Iterator it = selector.selectedKeys().iterator();
					
					while (it.hasNext()) {
						SelectionKey key = (SelectionKey)it.next();
						
						if (key.isAcceptable()) {
							SocketChannel socketChannel
								= serverChannel.accept();
							socketChannel.register(selector, SelectionKey.OP_READ);
						}
						
						if (key.isReadable()) {
							
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
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
