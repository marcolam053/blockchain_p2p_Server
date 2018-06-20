import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class PeriodicHeartBeatRunnable implements Runnable{

    private HashMap<ServerInfo,Date> serverStatus;
    private int sequenceNumber;
    private int localPort;

    public PeriodicHeartBeatRunnable(HashMap<ServerInfo,Date> serverStatus, int sequenceNumber,int localPort){
        this.serverStatus = serverStatus;
        this.sequenceNumber = sequenceNumber;
        this.localPort = localPort;
    }

    public void run(){

        while(true){
            String message = "hb|"+localPort+"|"+sequenceNumber+"\n";
            ArrayList<Thread> threadArrayList = new ArrayList<>();

            for(ServerInfo serverInfo : serverStatus.keySet()){
                HeartBeatClientRunnable hbc = new HeartBeatClientRunnable(serverInfo,message);
                Thread thread = new Thread(hbc);
                threadArrayList.add(thread);
                thread.start();
            }

            sequenceNumber++;

            try{
                Thread.sleep(2000);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
