package process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import transactionalIO.TransactionalFileInputStream;
import transactionalIO.TransactionalFileOutputStream;

public class FileCompress implements MigratableProcess{
	private boolean suspending;
	private boolean finished;
	private TransactionalFileInputStream input;
	private TransactionalFileOutputStream output;
	private String inputFileName;
	private int sleepSlot;
	
	public FileCompress(String[] args) throws IOException, FileNotFoundException, Exception, NumberFormatException {
		this.suspending = false;
		this.finished = false;
		File srcFile = new File(args[0]);
		File dstFile = new File(args[1]);
		if (!srcFile.exists()) {
			System.out.println("\t\t\tFileCompression():\tusage:\trun process.FileCompression <path-to-src-file> <path-to-src-file>");
			throw new FileNotFoundException();
		}
		if (!srcFile.isFile()) {
			System.out.println("\t\t\tFileCompression():\tInvalid arguments. Expect a srcfile instead of a directory.");
			System.out.println("\t\t\tFileCompression():\tusage:\trun process.FileCompression <path-to-src-file> <path-to-src-file>");
			throw new FileNotFoundException();
		}
		if (!dstFile.exists()) {
			if (!dstFile.createNewFile()) {
				System.out.println("\t\t\tFileCompression():\tUnable to create a new dstfile:" + dstFile.getAbsolutePath());
				throw new Exception();
			}
		}
		if (!dstFile.isFile()) {
			System.out.println("\t\t\tFileCompression():\tInvalid arguments. Expect a dstfile instead of a directory.");
			System.out.println("\t\t\tFileCompression():\tusage:\trun process.FileCompression <path-to-src-file> <path-to-src-file>");
			throw new FileNotFoundException();
		} else {
			if (!dstFile.delete()) {
				System.out.println("\t\t\tFileCompression():\tUnable to delete obselet dstfile:" + dstFile.getAbsolutePath());
				throw new Exception();
			} 
			if (!dstFile.createNewFile()) {
				System.out.println("\t\t\tFileCompression():\tUnable to create srcfile:" + dstFile.getAbsolutePath());
				throw new Exception();
			}
		}
		this.sleepSlot = Integer.parseInt(args[2]);
		if (this.sleepSlot < 0) {
			System.out.println("\t\t\tFileCompression():\tInvalid argument.Sleep slot should be non-negative.");
			throw new Exception();
		}
		this.input = new TransactionalFileInputStream(args[0],"rw");
		this.output = new TransactionalFileOutputStream(args[1],"rw");
	}
	@Override
	public void run() {
		int size = 1024;
		GZIPOutputStream gzipOutputStream = null;
		try {
			gzipOutputStream = new GZIPOutputStream(this.output, size);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int len;
		byte[] buff = new byte[size];
		while (!finished && !suspending) {
			try {
				len = input.read(buff);
				if (len > 0) {
					gzipOutputStream.write(buff, 0, len);
				} else {
					this.finished = true;
				}
			} catch (IOException e) {
				//TODO
				e.printStackTrace();
				this.finished = true;
				break;
			}
			if (this.sleepSlot == 0) {
				continue;
			}
			try {
				Thread.sleep(this.sleepSlot);
			} catch (InterruptedException e) {
				//TODO:
				e.printStackTrace();
				this.finished = true;
				break;
			}
		}
		try {
			if (this.finished) {
				input.close();
				output.close();
			}
			gzipOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.suspending = false;
		return;
		
	}

	@Override
	public void suspend() {
		this.suspending = true;
		while (this.suspending && !this.finished) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				System.err.println("\t\tWebCrawler.suspend():\tInterruption is called ");
			}
		}
	}

	@Override
	public int getStatus() {
		if (this.finished) {
			return 0;
		} else if (this.suspending) {
			return -1;
		} else {
			return 1;
		}
	}

}
/* Reference */
//[1].	http://www.mkyong.com/java/how-to-compress-files-in-zip-format/