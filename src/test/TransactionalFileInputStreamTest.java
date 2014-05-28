package test;

import java.io.IOException;

import TransactionalIO.TransactionalFileInputStream;

public class TransactionalFileInputStreamTest {
	public static void main (String[] args) {
		TransactionalFileInputStream file = new TransactionalFileInputStream("input_test.txt","r");
		int aByte = 0;
		try {
			while ((aByte = file.read()) != -1) {
				System.out.print((char)aByte + "");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
