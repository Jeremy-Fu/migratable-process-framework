package process;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class FindClass {
	public static void main (String[] args) {
		args = new String[4];
		args[0] = "run";
		args[1] = "process.WebCrawler";
		args[2] = "http://www.cmu.edu";
		args[3] = "webfile";
		
		String[] constructorArgs = new String[args.length - 2];
		for (int i = 0; i < constructorArgs.length; i++) {
			constructorArgs[i] = args[i + 2];
			String info = String.format("args[%d]:\t%s",i,constructorArgs[i]);
			System.out.println(info);
		}
	
		WebCrawler wc = null;
		try {
			wc = new WebCrawler(constructorArgs );
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		MigratableProcess migratableProcess = null;
		try {
			Class<MigratableProcess> aClass = (Class<MigratableProcess>) Class.forName(args[1]);
			Constructor<?> aConstructor = aClass.getConstructor(new Class[]{String[].class});
			if (aConstructor == null) {
				System.out.println("Constructor is null.");
			}
			
			Class<?>[] para = aConstructor.getParameterTypes();
			System.out.println("length = " + para.length);
			migratableProcess = (MigratableProcess) aConstructor.newInstance((Object)constructorArgs);
		} catch (ClassNotFoundException e) {
			String info = String.format("\tSlave.runProcess():\tThe requested class(%s) is not found.", args[1]);
			System.out.println(info);
			return;
		} catch (NoSuchMethodException e) {
			System.out.println("\tSlave.runProcess():\tMigratableProcess requires a constructor which takes String[] as parameters.");
			return;
		} catch (SecurityException e) {
			System.out.println("\tSlave.runProcess():\tAccess to the constructor is denied.");
			return;
		} catch (InstantiationException e) {
			System.out.println("\tSlave.runProcess():\tThe Constructor object is inaccessible or the Class is abstract.");
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			System.out.println("\tSlave.runProcess():\tIllegal prarameter is passed.");
			return;
		} catch (InvocationTargetException e) {
			System.out.println("\tSlave.runProcess():\tUnderlying constructor throws an exception.");
			return;
		} catch (ExceptionInInitializerError e) {
			System.out.println("\tSlave.runProcess():\tInitialization of this method failed.");
			return;
		} catch (IllegalAccessException e) {
			System.out.println("\tSlave.runProcess():\tConstructor object is enforcing Java language access control and the underlying constructor is inaccessible.");
			return;
		}
		
		if (migratableProcess == null) {
			return;
		}
		
		Thread newThread = new Thread(migratableProcess);
		newThread.start();
	}
	
}
