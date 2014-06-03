package transactionalIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;


public class TransactionalFileInputStream extends InputStream implements Serializable {
	
	private long counter = 0L;
	private String filename = "";
	private String permission = "";
	
	public TransactionalFileInputStream (String filename, String permission) {
		this.counter = 0L;
		this.filename = filename;
		this.permission = permission;
	}
	
	@Override
	/**
	 * read(): read a byte from current file pointer
	 * @return: The byte read from the file
	 * @author JeremyFu
	 */
	public int read() throws IOException {
		RandomAccessFile file = new RandomAccessFile(this.filename, this.permission);
		file.seek(this.counter);
		int aByte = file.read();
		if (aByte != -1) {
			this.counter++;
		}
		file.close();
		return aByte;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		RandomAccessFile file = new RandomAccessFile(this.filename, this.permission);
		file.seek(this.counter);
		int rst = file.read(b);
		if (rst == -1) {
			return -1;
		} else {
			this.counter += rst;
			file.close();
			return rst;
		} 
	}

}
