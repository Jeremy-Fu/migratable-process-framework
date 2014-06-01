package transactionalIO;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class TransactionalFileOutputStream extends OutputStream {
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

}
