import java.net.Socket;


public class Master implements Runnable{
	
	private Socket clientSocket;
	public Master(Socket socket) {
		this.clientSocket = socket;
	}
	
	@Override
	public void run() {
		
		
	}

}
