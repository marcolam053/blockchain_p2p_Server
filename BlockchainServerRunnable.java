import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

public class BlockchainServerRunnable implements Runnable {

    private Socket clientSocket;
    private Blockchain blockchain;
    private HashMap<ServerInfo, Date> serverStatus;
    private int localport;

    public BlockchainServerRunnable(Socket clientSocket, Blockchain blockchain, HashMap<ServerInfo, Date> serverStatus, int localPort) {
        this.clientSocket = clientSocket;
        this.blockchain = blockchain;
        this.serverStatus = serverStatus;
        this.localport = localPort;
    }

    public void run() {
        try {
            String remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
            serverHandler(clientSocket.getInputStream(), clientSocket.getOutputStream(), remoteIP);
            clientSocket.close();
        } catch (IOException e) {
        }
    }

    public void serverHandler(InputStream clientInputStream, OutputStream clientOutputStream, String remoteIP) {

        BufferedReader inputReader = new BufferedReader(
                new InputStreamReader(clientInputStream));
        PrintWriter outWriter = new PrintWriter(clientOutputStream, true);

        try {
            while (true) {
                String inputLine = inputReader.readLine();
                if (inputLine == null) {
                    break;
                }

                String[] tokens = inputLine.split("\\|");
                switch (tokens[0]) {
                    case "tx":
                        if (blockchain.addTransaction(inputLine))
                            outWriter.print("Accepted\n\n");
                        else
                            outWriter.print("Rejected\n\n");
                        outWriter.flush();
                        break;
                    case "pb":
                        outWriter.print(blockchain.toString() + "\n");
                        outWriter.flush();
                        break;
                    case "cc":
                        return;
                    case "hb":
                        heartBeat(tokens, remoteIP);
                        break;
                    case "si":
                        heartBeat(tokens, remoteIP);
                        break;
                    case "lb":
                        lb(tokens, remoteIP);
                    case "cu":
                        catchup(tokens);
                    default:
                        outWriter.print("Error\n\n");
                        outWriter.flush();
                }
            }
        } catch (IOException e) {

        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    private void catchup(String[] tokens) {
        // TODO : catchup server handler
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

            if (tokens.length == 2) {
                Block curr = blockchain.getHead();
                clientSocket.close();

                while (true) {
                    ServerSocket soc = new ServerSocket();
                    clientSocket = soc.accept();
                    outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                    if (curr != null) {

                        if (Base64.getEncoder().encodeToString(curr.calculateHash()).equals(tokens[1])) {
                            outputStream.writeObject(curr);
                            outputStream.flush();
                            outputStream.close();
                            clientSocket.close();
                            return;
                        }

                        outputStream.writeObject(curr);
                        outputStream.flush();
                        outputStream.close();
                        clientSocket.close();
                    }
                }
            } else if (tokens.length == 1) {

                Block curr = blockchain.getHead();
                outputStream.writeObject(curr);
                outputStream.flush();
                outputStream.close();
                clientSocket.close();
                return;
            }

        } catch (IOException e) {
        }

    }

    private void lb(String[] tokens, String remoteIP) {
        // TODO : LB handler

        try {

            String hashString = getHashString(blockchain);

            if (blockchain.getLength() < Integer.parseInt(tokens[2]) || hashString.length() < tokens[3].length()) {

                // Catch Up
                Socket soc = new Socket(remoteIP, Integer.valueOf(tokens[1]));
                PrintWriter printWriter = new PrintWriter(soc.getOutputStream(), true);

                ArrayList<Block> blocks = new ArrayList<>();
                printWriter.printf("cu" + "\n");

                ObjectInputStream inputStream = new ObjectInputStream(soc.getInputStream());
                Block b = (Block) inputStream.readObject();
                inputStream.close();
                soc.close();

                blocks.add(b);

                byte[] prevh = b.getPreviousHash();
                String prevHash = Base64.getEncoder().encodeToString(prevh);

                // if hash is not null, continue
                while (!prevHash.startsWith("A")) {
                    soc = new Socket(remoteIP, Integer.valueOf(tokens[1]));
                    printWriter = new PrintWriter(soc.getOutputStream(), true);

                    printWriter.printf("cu|%s\n", prevHash);

                    inputStream = new ObjectInputStream(soc.getInputStream());

                    b = (Block) inputStream.readObject();
                    inputStream.close();
                    soc.close();

                    blocks.add(b);

                    prevh = b.getPreviousHash();
                    prevHash = Base64.getEncoder().encodeToString(prevh);
                }

                // reconstruct blockchain
                blockchain = reconstruct(blockchain, blocks);
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void heartBeat(String[] tokens, String remoteIP) {
        String cmd = tokens[0];
        switch (cmd) {

            case "hb":
                int port = Integer.parseInt(tokens[1]);
                ServerInfo curr = new ServerInfo(remoteIP, port);


                if (serverStatus.containsKey(curr) == false) {
                    String forwardmsg = "si|" + localport + "|" + remoteIP + "|" + port+"\n";

                    // Unicast
                    ArrayList<Thread> threadArrayList = new ArrayList<>();
                    for (ServerInfo server : serverStatus.keySet()) {
                        Thread thread = new Thread(new HeartBeatClientRunnable(server, forwardmsg));
                        threadArrayList.add(thread);
                        thread.start();
                    }
                    for (Thread thread : threadArrayList) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            System.out.printf("INTERRUPTED THREAD JOINING\n");
                        }
                    }
                }

                serverStatus.put(curr, new Date());
                remove();

            case "si":
                int p = Integer.parseInt(tokens[1]);

                ServerInfo p_server = new ServerInfo(tokens[2], Integer.parseInt(tokens[3]));
                ServerInfo origin_server = new ServerInfo(remoteIP, p);
                ArrayList<ServerInfo> exception = new ArrayList<>();


                if (serverStatus.containsKey(p_server) == false) {
                    String msg = "si|" + localport + "|" + tokens[2] + "|" + tokens[3]+"\n";
                    exception.add(p_server);
                    exception.add(origin_server);

                    // Broadcast to all server except origin and the curr
                    ArrayList<Thread> threadArrayList = new ArrayList<>();
                    for (ServerInfo serverInfo : serverStatus.keySet()) {
                        if (exception.contains(serverInfo)) {
                            continue;
                        } else {
                            Thread thread = new Thread(new HeartBeatClientRunnable(serverInfo, msg));
                            threadArrayList.add(thread);
                            thread.start();
                        }
                    }

                    for (Thread thread : threadArrayList) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            System.out.printf("INTERRUPTED THREAD JOINING\n");
                        }
                    }

                    serverStatus.put(p_server, new Date());
                    serverStatus.put(origin_server, new Date());
                    remove();
                }
        }
    }

    // Helper Methods
    private String getHashString(Blockchain blockchain) {
        String hashString = "null";
        if (blockchain.getHead() != null) {
            byte[] hash = blockchain.getHead().calculateHash();
            hashString = Base64.getEncoder().encodeToString(hash);
        }
        return hashString;
    }

    private Blockchain reconstruct(Blockchain blockchain, ArrayList<Block> blocks) {
        blockchain.setHead(blocks.get(0));
        blockchain.setLength(blocks.size());

        Block cur = blockchain.getHead();

        int i = 0;
        while(i < blocks.size()) {
            if (i < blocks.size()) {
                Block prev = blocks.get(i + 1);
                cur.setPreviousBlock(prev);
            } else {
                cur.setPreviousBlock(null);
            }
            cur = cur.getPreviousBlock();
            i++;
        }
        return blockchain;
    }

    private void remove() {
        for (ServerInfo server : serverStatus.keySet()) {
            long curr_time = new Date().getTime();
            long diff = curr_time - serverStatus.get(server).getTime();
            if (diff > 4000) {
                serverStatus.remove(server);
            }
        }
    }
}