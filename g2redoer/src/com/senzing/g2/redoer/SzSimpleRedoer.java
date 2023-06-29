package com.senzing.g2.redoer;

// Imports
import com.senzing.g2.engine.G2JNI;
import com.senzing.g2.engine.Result;

import java.io.StringReader;

import java.util.concurrent.*;
import java.util.Set;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.HashSet;


public class SzSimpleRedoer {

    public static void main(String[] args){
        int INTERVAL = 100;
        
        String longRecord = System.getenv("LONG_RECORD");
        long LONG_RECORD = (longRecord!=null) ? Long.parseLong(longRecord)*1000l: 300l*1000l;
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
        g2.init("sz_simple_redoer_java", engineConfig, false);
	
        String threads = System.getenv("SENZING_THREADS_PER_PROCESS");
        int maxWorkers = 4;
        if(threads != null){
            maxWorkers = Integer.parseInt(threads);}

        int messages = 0;

        ExecutorService executor = Executors.newFixedThreadPool(maxWorkers);
        System.out.println("Threads: " + maxWorkers);
        int emptyPause = 0;
        
	IdentityHashMap<Future<String>,FutureData> futures=new IdentityHashMap<>();
	CompletionService<String> compService = new ExecutorCompletionService<String>(executor);
	Future<String> doneFuture = null;
	long logCheckTime = System.currentTimeMillis();
	long prevTime = logCheckTime;
	try{
		while(true){
			if(!futures.isEmpty()){
				doneFuture = compService.poll(10, TimeUnit.SECONDS);
				while(doneFuture!=null){
					messages++;
					futures.remove(doneFuture);
					doneFuture = compService.poll();
				}
			}
			if(messages%INTERVAL==0 && messages != 0){
				double diff = (System.currentTimeMillis()-prevTime)/1000.0;
				double speed = (diff>0.0) ? ((double)INTERVAL/diff): 0;
				System.out.printf("Processed " + String.valueOf(messages) + " redo, %.0f records per second\n", speed);
				prevTime = System.currentTimeMillis();
			}
			
			if(System.currentTimeMillis()>(logCheckTime+(LONG_RECORD/2))){
				int numStuck = 0;
				System.out.println(g2.stats());
				for (Future<String> future: futures.keySet()) {
					FutureData longRecordData= futures.get(future);
					long time = longRecordData.time;
					if(LONG_RECORD <= System.currentTimeMillis() - time){
						String longRecordMsg = longRecordData.message;
						System.out.printf("This record has been processing for %.2f minutes\n", (System.currentTimeMillis()-time)/(1000.0*60.0));
						System.out.println(longRecordMsg);
						numStuck++;
					}
					if(numStuck>=maxWorkers){
						System.out.println("All " + String.valueOf(maxWorkers) + " threads are stuck on long records");
					}
				}
				logCheckTime=System.currentTimeMillis();
			}
		        //Add processing the messages to the queue until the amount in the queue is equal to the number of workers.
		        while(futures.size()<maxWorkers){
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
		            FutureData futDat = new FutureData(msg);
		            Future<String> putFuture = compService.submit(() -> processMsg(g2, msg, true, futDat));
		            futures.put(putFuture, futDat);
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

    private static String processMsg(G2JNI engine, String msg, boolean withInfo, FutureData futDat){
        int returnCode = 0;
        futDat.time = System.currentTimeMillis();
        if(withInfo){
            StringBuffer response = new StringBuffer();
            returnCode = engine.processWithInfo(msg, 0, response);
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
            return null;
        }
    }
}
