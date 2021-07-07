package osiModel;

import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

import client.Client;
import node.Node;

public class Application {
	
    private final String userInterface = "Input: pm NODE MESSAGE to sent message to single NODE . \n"
    		+ "Input: gm MESSAGE to broadcast message.\n"
    		+ "Input: help for calling this interface again. \n";
    
    private final String GROUP_CHAT_KEY = "@@";
    
    private Client client;
    private Transport lowerLayer;
    private boolean running;
    private Scanner scanner;
    private Map<Integer,String> usersMap;
    
    public Application(Client client) {
        this.client = client;
        this.running = true;
        usersMap = Node.getNodesMap();
    }
    
    public void start() {
        Node myUser = Node.getNode(client.getAddress());
        System.out.println(myUser.getNodeColor() + " NODE ready");
        System.out.println(userInterface);
        startTerminalHandler();
    }
    
    public void setLowerLayer(Transport transportLayer) {
        this.lowerLayer = transportLayer;
    }
    
    public synchronized void receiveFromLowerLayer(String message, int address) { // -1 means from the system
        if (address == Node.SYSTEM_IP) {
            System.out.println(message);
        } else {
            if (message.substring(0, 2).equals(GROUP_CHAT_KEY)) {
                message = message.substring(2);
                Node sender = Node.getNode(address);
                System.out.println("GROUP - " + sender.getNodeColor() +": " + message);
            } else {
                Node sender = Node.getNode(address);
                System.out.println(sender.getNodeColor() + ": " + message);
            }
            
        }
    }
    
    private void startTerminalHandler() {
        scanner = new Scanner(System.in);
        while (running) {
            if (scanner.hasNextLine()) {
                String userInput = scanner.nextLine();
                if (userInput.charAt(0) == 'p' || userInput.charAt(0) == 'g' || userInput.charAt(0) == 'h') {
                    handleCommand(userInput);
                } else {
                    System.out.println("Invalid command.");
                }
            }
        }
    }
    
    private void handleCommand(String userInput) {
        String[] splits = userInput.split(" ");
        if (splits.length > 0) {
            switch(splits[0]) {
            case "help":
                System.out.println(userInterface);
                break;

            case "pm":
                sendMessage(splits);
                break;
                
            case "gm":
                sendGroupMessage(splits);
                break;
                
            default:
                System.out.println("First part: " + splits[0]);
                System.out.println("Command: " + String.join(" ", userInput));
                System.out.println("Invalid Command! Type /help for support. 1");
                break;
            }
        }
    }
    
    private void sendGroupMessage(String[] userInput) {
        if (userInput.length <= 1) {
            System.out.println("Invalid Command!");
            return;
        }
        String[] messageParts = Arrays.copyOfRange(userInput, 1, userInput.length);
        String message = GROUP_CHAT_KEY + String.join(" ", messageParts);
        System.out.println("Sending broadcast...");
        lowerLayer.receiveFromUpperLayer(message,Node.GROUP_CHAT);
    }
    
    private void sendMessage(String[] userInput) {
        if (userInput.length <= 1) {
            System.out.println("Invalid command!");
            return;
        }
        String node = userInput[1];
        if (node.equals( Node.getNode(client.getAddress()).getNodeColor())) {
            System.out.println("Please choose another node.");
            return;
        }
        String[] messageParts = Arrays.copyOfRange(userInput, 2, userInput.length);
        
        String message = String.join(" ", messageParts);
        if (this.usersMap.containsValue(node)) {
            System.out.println("Sending message...");
            this.lowerLayer.receiveFromUpperLayer(message, Node.findIpByNodeColor(node));
        } else {
            System.out.println("NODE not exist!");
        }
    }
}
