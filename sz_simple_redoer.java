// Imports
import com.senzing.g2.engine.G2JNI;
import com.senzing.g2.engine.Result;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
//********

public class simpleRedoer {

    public static void main(String[] args){
        static int INTERVAL = 1000;
        string longRecord = System.getenv("LONG_RECORD");
        if(longRecord != NULL)
            static int LONG_RECORD = Integer.parseInt(longRecord);
        else
            static int LONG_RECORD = 300;
        string pauseTime = System.getenv("SENZING_REDO_SLEEP_TIME_IN_SECONDS");
        if(pauseTime != NULL)
            static int EMPTY_PAUSE_TIME = Integer.parseInt(pauseTime);
        else
            static int EMPTY_PAUSE_TIME = 60;

        
        System.exit(0);
    }


    private string processMsg(G2JNI engine, string msg, string info){
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
