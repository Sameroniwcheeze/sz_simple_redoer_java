// Imports
import com.senzing.g2.engine.G2JNI;
import com.senzing.g2.engine.Result;

import java.time;
import java.io.StringReader;

import java.util.concurrent.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
//********

public class simpleRedoer {

    private Clock time;
    public static void main(String[] args){
        static int INTERVAL = 1000;
        String longRecord = System.getenv("LONG_RECORD");
        if(longRecord != NULL)
            static int LONG_RECORD = Integer.parseInt(longRecord);
        else
            static int LONG_RECORD = 300;
        String pauseTime = System.getenv("SENZING_REDO_SLEEP_TIME_IN_SECONDS");
        if(pauseTime != NULL)
            static int EMPTY_PAUSE_TIME = Integer.parseInt(pauseTime);
        else
            static int EMPTY_PAUSE_TIME = 60;

        String logLevel = System.getenv("SENZING_LOG_LEVEL");
        if(logLevel!=NULL)
            String SENZING_LOG_LEVEL = logLevel;
        else
            String SENZING_LOG_LEVEL = "info";

        //Setup info and logging

        engineConfig = os.getenv("SENZING_ENGINE_CONFIGURATION_JSON");

        if(engineConfig == NULL){
            System.out.println("The environment variable SENZING_ENGINE_CONFIGURATION_JSON must be set with a proper JSON configuration.");
            System.out.println("Please see https://senzing.zendesk.com/hc/en-us/articles/360038774134-G2Module-Configuration-and-the-Senzing-API");
            System.exit(-1);
        }

        G2JNI g2 = new G2JNI();
        g2.init("sz_simple_redoer", engineConfig, args.debugTrace);
        int logCheckTime = int prevTime = time.instant();

        string threads = System.getenv("SENZING_THREADS_PER_PROCESS");
        if(threds != NULL)
            int max_workers = Integer.parseInt(threads);
        else
            int max_workers = 0;

        int messages = 0;

        ExecutorService executor = Executors.newFixedThreadPool(max_workers);
        System.out.println("Threads: " + executor.max_workers());
        int emptyPause = 0;

        List<Future<String, Integer>> futures = NULL;
        try{
            while(true){

                int nowTime = time.instant();

                if(futures!=NULL){
                    futures.get(10, TimeUnit.SECONDS);

                    //delete_batch = [];
                    int deleteCnt = 0;
                    
                    for(int i = 0; i<futures.size(); i++){
                        String msg = futures.get(0);
                        futures.remove(0);

                        try{

                        }
                        catch(Exception e){

                        }
                    }
                }

                //Add processing the messages to the queue until the amount in the queue is equal to the number of workers.
                while(futures.size()<max_workers){
                    try{
                        StringBuffer response = new StringBuffer();
                        g2.getRedoRecord(response);
                        if(response==NULL)
                            System.out.println("No redo records available. Pausing for " + EMPTY_PAUSE_TIME + " seconds.");
                            int emtpy_pause = time.instant() + EMPTY_PAUSE_TIME;
                            break;
                    }
                    String msg = response.toString();
                    futures[executor.submit(new processMsg(g2, msg, SENZING_LOG_LEVEL))] = (msg, time.instant());
                    catch(Exception e){
                        System.out.println(e);
                    }
                }
            }
        }
        catch(Exception e){
            System.out.println(e);
        }

        
        System.exit(0);
    }

    private string processMsg(G2JNI engine, String msg, String info){
        int returnCode = 0;
        if(info != NULL){
            Stringbuffer response = new StringBuffer();
            returnCode = engine.processWithInfo(msg, response);
            if(returnCode!=0){
                System.out.println("Exception " + g2engine.getLastException() + " on message: " + msg);
                break;
            }
            return response.toString();
        }
        else{
            returnCode = engine.process(msg);
            if(returnCode!=0)
                System.out.println("Exception " + g2engine.getLastException() + " on message: " + msg);
        }
        return NULL;
    }
}