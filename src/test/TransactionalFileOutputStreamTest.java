package test;

import java.io.IOException;

import transactionalIO.TransactionalFileOutputStream;

public class TransactionalFileOutputStreamTest {
	public static void main (String[] args) {
		TransactionalFileOutputStream file = new TransactionalFileOutputStream("output_test.txt","rw");
		byte[] output = {65, 66, 67, 68, 69, 10, 70, 71, 72};
		try {
			for (int i = 0; i < output.length; i++) {
				file.write(output[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
