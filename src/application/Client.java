package application;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Client implements Serializable {
	ArrayList<Integer> rst = new ArrayList<Integer>();
	TestClass nested = new TestClass();

	public static void main (String[] args) {
		Client client = new Client();
		client.rst.add(1);
		client.rst.add(2);
		client.rst.add(3);
		try {
			FileOutputStream fileOut =
		         new FileOutputStream("arraylist.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(client);
			out.close();
			fileOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
