package com.senzing.g2.redoer;

// Imports
import com.senzing.g2.engine.G2JNI;
import com.senzing.g2.engine.Result;

import java.time.Instant;
import java.io.StringReader;

import java.util.concurrent.*;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

/*
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;*/
//********

public class sz_simple_redoer {

    public static void main(String[] args){
    	System.out.println("Making variables");
        int INTERVAL = 1000;
        String longRecord = System.getenv("LONG_RECORD");
        int LONG_RECORD = 300;
        if(longRecord != null){
            LONG_RECORD = Integer.parseInt(longRecord);}
        String pauseTime = System.getenv("SENZING_REDO_SLEEP_TIME_IN_SECONDS");
        int EMPTY_PAUSE_TIME = 60;
        if(pauseTime != null){
            EMPTY_PAUSE_TIME = Integer.parseInt(pauseTime);}

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
	System.out.println("Initalizing engine");
        G2JNI g2 = new G2JNI();
        g2.init("sz_simple_redoer", engineConfig, false);
        Instant logCheckTime = Instant.now();
	Instant prevTime = logCheckTime;
	
        String threads = System.getenv("SENZING_THREADS_PER_PROCESS");
        int max_workers = 8;
        if(threads != null){
            max_workers = Integer.parseInt(threads);}

        int messages = 0;

        ExecutorService executor = Executors.newFixedThreadPool(max_workers);
        System.out.println("Threads: " + max_workers);
        int emptyPause = 0;
        
	HashMap<Future<String>,String> futures=new HashMap<Future<String>,String>();
	CompletionService<String> CS = new ExecutorCompletionService<String>(executor);
	Future<String> doneFuture = null;
	try{
		while(true){

			if(!futures.isEmpty()){
				
				while(doneFuture == null){
					//doneFuture = CS.poll(10, TimeUnit.SECONDS);
					doneFuture = CS.poll(10, TimeUnit.SECONDS);
					System.out.println(doneFuture);
				}

				while(doneFuture!=null){
					System.out.println("Printing completed future(s)");
					System.out.println(doneFuture.get());
					futures.remove(doneFuture);
					doneFuture = CS.poll();
					messages++;
				}
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
		            
		            //int time = (int)Instant.toEpochMilli();
		            futures.put(CS.submit(() -> processMsg(g2, msg, SENZING_LOG_LEVEL)), msg);
			}
		}
	}
	catch(Exception e){
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
