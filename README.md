# blockchain_p2p_Server
University Assignment - Build a p2p blockchain application

# The application:
* Uses multithreading and Java Sockets to handle message and connection from different client simultaneously
* Enable peers to be both client and server at the same time.
* Tolerates the dynamism of the system and unreliability of the network using a catchup protocol

Given some skeleton files provided by the course coordinators, I implemented:
1. Heartbeat-based dynamic neighbor communication
   * Heartbeat sending
   * Heratbeat receving and server info sending
   * Server info receiving and server info relaying
2. Catchup protocol and Blockchain Consensus
   * Latest block message sending
   * Catchup message sending
   * Server catchup algorithm
   
# The given files are :
• A Transaction.java file.
• A Block.java file.
• A Blockchain.java file.
• A BlockchainServer.java file.
• A BlockchainServerRunnable.java file. 
• A PeriodicCommitRunnable.java file.
• A ServerInfo.java file.
