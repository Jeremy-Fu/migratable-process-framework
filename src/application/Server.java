package application;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class Server {
	public static void main (String[] args) {
		Client rcv = null;
				
		try {
			FileInputStream fileIn = new FileInputStream("arraylist.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			rcv = (Client) in.readObject();
			in.close();
			fileIn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (rcv == null) {
			System.out.println("Read Client object failed.");
			return;
		}
		
		System.out.println("nested:\tname:\t" + rcv.nested.name);
		System.out.println("\t\tversion:\t" + rcv.nested.version);
		for (int i = 0; i < rcv.rst.size();i++) {
			System.out.println("\t\telement " + i + ":\t" + rcv.rst.get(i));
		}
	}
}
