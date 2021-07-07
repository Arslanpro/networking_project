package node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node {

	private int ip;
	private String color;
	public final static String RED_NODE = "RED";
	public final static String GREEN_NODE = "GREEN";
	public final static String BLUE_NODE = "BLUE";
	public final static String CYAN_NODE = "CYAN";
	public final static int SYSTEM_IP = -1;
	public final static int RED_IP = 0;
	public final static int GREEN_IP = 1;
	public final static int BLUE_IP = 2;
	public final static int CYAN_IP = 3;
	public final static int GROUP_CHAT = 4;

	public Node(int ip, String color) {
		this.ip = ip;
		this.color = color;
	}

	public int getIp() {
		return this.ip;
	}

	public String getNodeColor() {
		return this.color;
	}
	
    public static Node getNode(int nodeIp) {
        switch (nodeIp) {
        case RED_IP:
            return new Node(RED_IP,RED_NODE);
        case GREEN_IP:
            return new Node(GREEN_IP,GREEN_NODE);
        case BLUE_IP:
            return new Node(BLUE_IP,BLUE_NODE);
        case CYAN_IP:
            return new Node(CYAN_IP,CYAN_NODE);
        }
        return null;
    }
    
    public static List<Integer> getOtherNodesIPList(int Ip) {
        List<Integer> otherNodesIP = new ArrayList<Integer>();
        for (int i = 0;i<4;i++) {
            if (i == Ip) {
                continue;
            }
            otherNodesIP.add(i);
        }
        return otherNodesIP;
    }
    
    
    public static Map<Integer, String> getNodesMap() {
        Map<Integer, String> nodes = new HashMap<Integer,String>();
        nodes.put(RED_IP, RED_NODE);
        nodes.put(BLUE_IP, BLUE_NODE);
        nodes.put(GREEN_IP, GREEN_NODE);
        nodes.put(CYAN_IP, CYAN_NODE);
        return nodes;
    }
    
    public static int findIpByNodeColor(String color) {
        switch (color) {
        case RED_NODE: return RED_IP;
        case BLUE_NODE: return BLUE_IP;
        case GREEN_NODE: return GREEN_IP;
        case CYAN_NODE: return CYAN_IP;
        default: return -1;
        }
    }
}
