import java.io.*;
import java.net.*;

public class HeartBeatClientRunnable implements Runnable {

    private ServerInfo serverStatus;
    private String message;

    public HeartBeatClientRunnable(ServerInfo serverStatus,String message){
            this.serverStatus = serverStatus;
            this.message = message;
    }

    public void run(){
        try{
            Socket soc = new Socket();
            soc.connect(new InetSocketAddress(serverStatus.getHost(),serverStatus.getPort()),2000);
            PrintWriter printWriter = new PrintWriter(soc.getOutputStream(),true);

            printWriter.printf(message+"\n");
            printWriter.close();
            soc.close();

            try{
                Thread.sleep(2000);
            } catch(InterruptedException e){
                System.out.printf("INTERRUPTED THREAD JOINING\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
