package util;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadManager {
	private static final int cores = Runtime.getRuntime().availableProcessors()-1;
	private static final Thread[] threads = new Thread[cores];
	private static final boolean [] runningThreads = new boolean[cores];
	private static int activeThreads;
	static ReentrantLock activeThreadslock = new ReentrantLock(true);

	synchronized public static void scheduleRunnable(final Runnable r) {
		int openId = -1;
		for(int i=0;i<threads.length;i++) 
			if(!runningThreads[i]) 
				openId = i;
		if(openId==-1)
			throw new RuntimeException("Failed to schedule runnable: must wait for next schedule opening");
		activeThreadslock.lock();
		activeThreads++;
		activeThreadslock.unlock();
		
		final int threadId = openId;
		threads[threadId] = new Thread() {
			@Override
			public void run() {
				runningThreads[threadId] = true;
				r.run();
				runningThreads[threadId] = false;
				activeThreadslock.lock();
				activeThreads--;
				activeThreadslock.unlock();
			}
		};
		threads[threadId].start();
	}
	synchronized public static boolean canScheduleRunnable() {
		activeThreadslock.lock();
		boolean ret = activeThreads<threads.length;
		activeThreadslock.unlock();
		return ret;
	}
	synchronized public static void waitForNextScheduleOpening() {
		while(true) {
			if(canScheduleRunnable())
				return;
			try {
				Thread.sleep(1);
			}
			catch (InterruptedException e) {
				//e.printStackTrace();
			}
		}
	}
	synchronized public static void synchronizeAll() {
		for(int i=0;i<threads.length;i++) 
			if(!runningThreads[i])
				try {
					if(threads[i]!=null)
					threads[i].join();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
	}
}
