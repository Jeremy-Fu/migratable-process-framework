package transactionalIO;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

public class TransactionalFileOutputStream extends OutputStream implements Serializable {
	private String filename;
	private long counter;
	private String permission;
	
	public TransactionalFileOutputStream (String filename, String permission) {
		this.counter = 0L;
		this.filename = filename;
		this.permission = permission;
	}
	
	@Override
	public void write(int aByte) throws IOException {
		RandomAccessFile file = new RandomAccessFile(this.filename, this.permission);
		file.seek(this.counter);
		file.write(aByte);
		counter++;
		file.close();
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		RandomAccessFile file = new RandomAccessFile(this.filename, this.permission);
		file.seek(this.counter);
		for (int i = 0; i < b.length; i++) {
			file.write(b[i]);
		}
		counter += b.length;
		file.close();
	}

}
