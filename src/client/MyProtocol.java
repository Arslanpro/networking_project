package client;

import java.util.concurrent.BlockingQueue;

import node.Node;
import osiModel.Application;
import osiModel.DataLink;
import osiModel.Network;
import osiModel.Transport;

//Jiangwei Yao s2365065
//Wenjie Zhao s2198312

public class MyProtocol {
 // The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys2.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 15600;//TODO: Set this to your group frequency!
    
    //use different NODE_IP for each processes
    protected static int NODE_IP = Node.RED_IP; 
//  protected static int NODE_IP = Node.GREEN_IP; 
//  protected static int NODE_IP = Node.BLUE_IP; 
//  protected static int NODE_IP = Node.CYAN_IP; 
    
    private Client client;
    private Application applicationLayer;
    private Transport transportLayer;
    private Network networkLayer;
    private DataLink linkLayer;
    
 

    public MyProtocol(String server_ip, int server_port, int frequency){
    	
        client = new Client(SERVER_IP, SERVER_PORT, frequency); // Give the client the Queues to use
        
        applicationLayer = new Application(client);
        transportLayer = new Transport(client);
        networkLayer = new Network(client);
        linkLayer = new DataLink(client);
        
        applicationLayer.setLowerLayer(transportLayer);
        transportLayer.setUpperLayer(applicationLayer);
        transportLayer.setLowerLayer(networkLayer);
        networkLayer.setUpperLayer(transportLayer);
        networkLayer.setLowerLayer(linkLayer);    
        linkLayer.setUpperLayer(networkLayer);
        linkLayer.setLowerLayer(this);
        
        new receiveThread(client.getReceivedQueue(), linkLayer).start(); // Start thread to handle received messages!

        // handle sending from stdin from this thread.
        applicationLayer.start();
    }

    public static void main(String args[]) {
        if(args.length > 0){
            frequency = Integer.parseInt(args[0]);
        }
        new MyProtocol(SERVER_IP, SERVER_PORT, frequency);        
    }
    
    public void receiveFromUpperLayer(Message message) {
        if (message != null) {
            try {
                client.getSendingQueue().put(message);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("PL - NULL MESSAGE");
        }
    }

    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;
        private DataLink linkLayer;

        public receiveThread(BlockingQueue<Message> receivedQueue, DataLink linkLayer){
            super();
            this.setName("Thread-Receiving");
            this.receivedQueue = receivedQueue;
            this.linkLayer = linkLayer;
        }

        public void run(){
            while(true) {
                try{
                    Message msg = receivedQueue.take();
                    linkLayer.receiveMessage(msg);
                } catch (InterruptedException e){
                    System.err.println("Failed to take from queue: "+e);
                }                
            }
        }
    }
}
