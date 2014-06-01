package manager;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.Iterator;

public class Master implements Runnable{
	
	private Hashtable<String, SocketChannel> slaveInfo = null;
	private int port = 22045;
	private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
	
	public Master(int port) {
		this.port = port;
		this.slaveInfo = new Hashtable<String,SocketChannel>();
	}
	
	@Override
	public void run() {
		try {
			/* Setup server socket (waiting for upcoming slaves) */
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			ServerSocket serverSocket = serverSocketChannel.socket();
			serverSocket.bind(new InetSocketAddress(this.port));
			
			/* Setup multiplexed I/O (waiting for both upcoming slaves registration and look-up) */
			Selector selector  = Selector.open();
			
			/* Register server channel with selector */
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		
			System.out.println("\tMaster.run():\tMaster server is listening at port:" + this.port);
			while (true) {
				//System.out.println("\tMaster.run():\tWait...");
				int n = selector.select();
				
				if (n == 0) {
					System.out.println("\tMaster.run():\tContinue...");
					continue;
				}
				
				Iterator it = selector.selectedKeys().iterator();
				
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey)it.next();
					
					if (key.isAcceptable()) {
						SocketChannel socketChannel
							= ((ServerSocketChannel)key.channel()).accept();
						socketChannel.configureBlocking(false);
						socketChannel.register(selector, SelectionKey.OP_READ);
						String newSlaveName = "slave" + (this.slaveInfo.size() + 1);
						
						/* Add the new slave to the slaveInfo */
						this.slaveInfo.put(newSlaveName, socketChannel);
						System.out.println("\tMaster.run():\tAccept a new slave.");
						/* Inform the new slave its name */
						buffer.clear();
						buffer.put((newSlaveName + "\n").getBytes());
						buffer.flip();
						socketChannel.write(buffer);
					}
					
					if (key.isReadable()) {
						
					}
					
					it.remove();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
