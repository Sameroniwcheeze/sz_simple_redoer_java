package com.senzing.g2.redoer;

// Imports
import com.senzing.g2.engine.G2JNI;
import com.senzing.g2.engine.Result;

import java.io.StringReader;

import java.util.concurrent.*;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;


/*
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;*/
//********

public class sz_simple_redoer {

    public static void main(String[] args){
        int INTERVAL = 100;
        
        String longRecord = System.getenv("LONG_RECORD");
        long LONG_RECORD = (longRecord!=null) ? Integer.parseInt(longRecord): 300000;

        String pauseTime = System.getenv("SENZING_REDO_SLEEP_TIME_IN_SECONDS");
        int EMPTY_PAUSE_TIME = (pauseTime!=null) ? Integer.parseInt(pauseTime): 60;

        String logLevel = System.getenv("SENZING_LOG_LEVEL");
        String SENZING_LOG_LEVEL = (logLevel!=null) ? logLevel: "info";

        //Setup info and logging
        String engineConfig = System.getenv("SENZING_ENGINE_CONFIGURATION_JSON");

        if(engineConfig == null){
            System.out.println("The environment variable SENZING_ENGINE_CONFIGURATION_JSON must be set with a proper JSON configuration.");
            System.out.println("Please see https://senzing.zendesk.com/hc/en-us/articles/360038774134-G2Module-Configuration-and-the-Senzing-API");
            System.exit(-1);
        }
        int returnCode = 0;
        G2JNI g2 = new G2JNI();
        g2.init("sz_simple_redoer", engineConfig, false);
	
        String threads = System.getenv("SENZING_THREADS_PER_PROCESS");
        int max_workers = 4;
        if(threads != null){
            max_workers = Integer.parseInt(threads);}

        int messages = 0;

        ExecutorService executor = Executors.newFixedThreadPool(max_workers);
        System.out.println("Threads: " + max_workers);
        int emptyPause = 0;
        
	HashMap<Future<String>,String> futures=new HashMap<Future<String>,String>();
	HashMap<Future<String>,Long> futuresTime=new HashMap<Future<String>,Long>();
	CompletionService<String> CS = new ExecutorCompletionService<String>(executor);
	Future<String> doneFuture = null;
	long logCheckTime = System.currentTimeMillis();
	long prevTime = logCheckTime;
	try{
		while(true){

			if(!futures.isEmpty()){
				doneFuture = CS.poll(10, TimeUnit.SECONDS);

				while(doneFuture!=null){

					System.out.print(doneFuture.get());
					futures.remove(doneFuture);
					System.out.println(" it took " + String.valueOf(-futuresTime.get(doneFuture)+System.currentTimeMillis()) + " mseconds");
					futuresTime.remove(doneFuture);
					doneFuture = CS.poll();
					messages++;
				}
			}
			
			if(messages%INTERVAL==0){
				long diff = System.currentTimeMillis()-prevTime;
				long speed = (diff>0) ? 1000*((long)INTERVAL)/diff: 0;
				System.out.println("Processed " + String.valueOf(messages) + " redo, " + String.valueOf(speed) + " records per second");
				prevTime = System.currentTimeMillis();
			}
			
			if(System.currentTimeMillis()>(logCheckTime+(LONG_RECORD/2))){
				int numStuck = 0;
				System.out.println(g2.stats());
				Set<Future<String>> runningFutures = futures.keySet();
				Iterator futureIt = runningFutures.iterator();
				while(futureIt.hasNext()){
					Future<String> key = (Future<String>)futureIt.next();
					long time = futuresTime.get(key);
					if(time >= System.currentTimeMillis() + LONG_RECORD){
						System.out.println("This record has been processing for " + String.valueOf((System.currentTimeMillis()-time)/(1000.0*60.0)) + " minutes");
						numStuck++;
					}
					if(numStuck>=max_workers){
						System.out.println("All " + String.valueOf(max_workers) + " threads are stuck on long records");
					}
				}
				logCheckTime=System.currentTimeMillis();
			}
		        //Add processing the messages to the queue until the amount in the queue is equal to the number of workers.
		        while(futures.size()<max_workers){
		                StringBuffer response = new StringBuffer();
		                returnCode = g2.getRedoRecord(response);
		                if(returnCode!=0){
		                    System.out.println("Exception " + g2.getLastException() + " on get redo.");
		                    System.out.println("Processed a total of " + String.valueOf(messages) + " messages");
		                    executor.shutdown();
		                    g2.destroy();
	       			    System.exit(0);
		                }
		                if(response.length()==0){
		                    System.out.println("No redo records available. Pausing for " + String.valueOf(EMPTY_PAUSE_TIME) + " seconds.");
				    TimeUnit.SECONDS.sleep(EMPTY_PAUSE_TIME);
				    break;
		                }
		            String msg = response.toString();
		            
		            Future<String> putFuture = CS.submit(() -> processMsg(g2, msg, SENZING_LOG_LEVEL));
		            futures.put(putFuture, msg);
		            futuresTime.put(putFuture, System.currentTimeMillis());
			}
		}
	}
	catch(Exception e){
	    System.out.println(e);
	    System.out.println("Processed a total of " + String.valueOf(messages) + " messages");
            executor.shutdown();
            g2.destroy();
	    System.exit(0);
	}
    }

    private static String processMsg(G2JNI engine, String msg, String info){
        int returnCode = 0;
        if(info != null){
            StringBuffer response = new StringBuffer();
            long g2DefaultFlag = engine.G2_RECORD_DEFAULT_FLAGS;	
            returnCode = engine.processWithInfo(msg, g2DefaultFlag, response);
            if(returnCode!=0){
                System.out.println("Exception " + engine.getLastException() + " on message: " + msg);
                return null;
            }
            return response.toString();
        }
        else{
            returnCode = engine.process(msg);
            if(returnCode!=0)
                System.out.println("Exception " + engine.getLastException() + " on message: " + msg);
        }
        return null;
    }
}
