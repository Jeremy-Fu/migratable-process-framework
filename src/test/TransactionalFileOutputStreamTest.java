package test;

import java.io.IOException;

import transactionalIO.TransactionalFileOutputStream;

public class TransactionalFileOutputStreamTest {
	public static void main (String[] args) {
		TransactionalFileOutputStream file = new TransactionalFileOutputStream("output_test.txt","rw");
		
		String output = "http://www.cmu.edu";
		try {
			file.write(output.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
