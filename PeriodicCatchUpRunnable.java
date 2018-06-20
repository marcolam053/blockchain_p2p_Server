// REFERENCE : https://stackoverflow.com/questions/8378752/pick-multiple-random-elements-from-a-list-in-java

import java.util.*;
import java.io.*;

public class PeriodicCatchUpRunnable implements Runnable{

    private Blockchain blockchain;
    private HashMap<ServerInfo,Date> serverStatus;
    private int localPort;

    public PeriodicCatchUpRunnable(Blockchain blockchain, HashMap<ServerInfo,Date> serverStatus, int localPort){
        this.blockchain = blockchain;
        this.localPort = localPort;
        this.serverStatus = serverStatus;
    }

    public void run(){

        while (true) {
            String msg = "lb|"+localPort+"|"+blockchain.getLength()+"|";
            msg = generateMessage(msg,blockchain,localPort);

            ArrayList<ServerInfo> servers = new ArrayList<>(serverStatus.keySet());
            ArrayList<ServerInfo> random = new ArrayList<>();

            if (serverStatus.size() > 5) {

                random = shuffle_server(servers,random);
                multicast(random,msg);

            } else if (serverStatus.size() <= 5) {
                broadcast(msg);
            }

            try{
                Thread.sleep(2000);
            } catch (InterruptedException e){
                System.out.printf("INTERRUPTED THREAD JOINING\n");
            }
        }
    }

    private ArrayList<ServerInfo> shuffle_server(ArrayList<ServerInfo> servers, ArrayList<ServerInfo> random) {
        int i = 0;
        while(i < 5){
            Collections.shuffle(servers);
            random.add(servers.remove(0));
            i++;
        }
        return random;
    }

    private String generateMessage(String msg,Blockchain blockchain,int localPort){

        if(blockchain.getLength() == 0){
            msg =  "lb|"+localPort+"|"+blockchain.getLength()+"|"+ Base64.getEncoder().encodeToString(new byte[32])+"\n";
        } else {
            byte[] hash = blockchain.getHead().calculateHash();
            if(hash != null){
                msg = "lb|"+localPort+"|"+blockchain.getLength()+"|"+ Base64.getEncoder().encodeToString(hash)+"\n";
            } else {
                msg = "lb|"+localPort+"|"+blockchain.getLength()+"|null\n";
            }
        }

        return msg;
    }

    private void broadcast(String msg){
        ArrayList<Thread> threadArrayList = new ArrayList<>();

        for(ServerInfo serverInfo : serverStatus.keySet()){
            HeartBeatClientRunnable hbc = new HeartBeatClientRunnable(serverInfo,msg);
            Thread thread = new Thread (hbc);
            threadArrayList.add(thread);
            thread.start();
        }

        for(Thread thread : threadArrayList){
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void multicast(ArrayList<ServerInfo> random, String msg) {
        ArrayList<Thread> threadArrayList = new ArrayList<>();

        for (ServerInfo server : random){
            HeartBeatClientRunnable hbc = new HeartBeatClientRunnable(server,msg);
            Thread thread = new Thread(hbc);
            threadArrayList.add(thread);
            thread.start();
        }

        for(Thread thread : threadArrayList){
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}