import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class BlockchainServer {
    public static void main(String[] args) {

        if (args.length != 3) {
            return;
        }

        int localPort = 0;
        int remotePort = 0;
        String remoteHost = null;

        try {
            localPort = Integer.parseInt(args[0]);
            remoteHost = args[1];
            remotePort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return;
        }

        Blockchain blockchain = new Blockchain();

        HashMap<ServerInfo, Date> serverStatus = new HashMap<ServerInfo, Date>();
        serverStatus.put(new ServerInfo(remoteHost, remotePort), new Date());

        PeriodicCommitRunnable pcr = new PeriodicCommitRunnable(blockchain);
        Thread pct = new Thread(pcr);
        pct.start();

        // periodic heartbeat start
        PeriodicHeartBeatRunnable periodicHeartBeatRunnable = new PeriodicHeartBeatRunnable(serverStatus,0,localPort);
        Thread phb = new Thread(periodicHeartBeatRunnable);
        phb.start();

        // catch up prior sending latest block
        catchUp_prior(remoteHost,remotePort);

        // Periodic Catchup Start
        PeriodicCatchUpRunnable periodicCatchUpRunnable = new PeriodicCatchUpRunnable(blockchain,serverStatus,localPort);
        Thread pcu = new Thread (periodicCatchUpRunnable);
        pcu.start();

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(localPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new BlockchainServerRunnable(clientSocket, blockchain, serverStatus,localPort)).start();
            }
        } catch (IllegalArgumentException e) {
        } catch (IOException e) {
        } finally {
            try {
                pcr.setRunning(false);
                pct.join();
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }
        }
    }

    private static void catchUp_prior(String remoteHost, int remotePort) {
        try{
            Socket soc = new Socket(remoteHost,remotePort);
            PrintWriter printWriter = new PrintWriter(soc.getOutputStream(), true);
            printWriter.printf("cu"+"\n");

            printWriter.close();
            soc.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
