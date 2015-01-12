package com.oracle.poc.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MultiThreadCreatePSRData {

	private  BlockingQueue <Runnable> createPSRQueue = new LinkedBlockingQueue<Runnable>();;
	private  ThreadPoolExecutor tpe = new ThreadPoolExecutor(15, 15, 0, TimeUnit.SECONDS, createPSRQueue, Executors.defaultThreadFactory());
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		MultiThreadCreatePSRData multiThreadCreatePSRData = new MultiThreadCreatePSRData();
		multiThreadCreatePSRData.executeTask();
		System.out.println("CreatePSRData.main(): COMPLETED*****");
		System.exit(0);
	}
	
	public void executeTask() throws InterruptedException{
		Collection<Future<?>> futures = new LinkedList<Future<?>>();
		ExecutorService es = Executors.newFixedThreadPool(3);
		
		for (int i = 0; i< 3; i++) {
			Runnable r = new PSRWorkerThread();
			futures.add(es.submit(r));
			tpe.execute(r);
		}

		while (tpe.getTaskCount() != tpe.getCompletedTaskCount() && ! tpe.getQueue().isEmpty()){
			synchronized (tpe) {
				tpe.wait(1000);
			}
		}
		
		try{
			for (Future<?> future:futures) {
			    future.get();
			}
			
		}catch(Exception e){
			
		}
		System.out.println("MultiThreadCreatePSRData.executeTask(): COMPLETED");
		
	}
	
	    
}
