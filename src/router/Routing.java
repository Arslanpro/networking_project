package router;

import java.util.ArrayList;
import java.util.List;

public class Routing {
    private int srcAddress;
    private int desAddress;
    private List<Integer> routing;
    
    public Routing(int srcAddress, int desAddress) {
        this.srcAddress = srcAddress;
        this.desAddress = desAddress;
        this.routing = new ArrayList<Integer>();
        routing.add(srcAddress);
    }    
    
    public void addNode(int address) {
        routing.add(address);
    }
    
    public List<Integer> getNextNodes(int currentNode) {
        int currentIndex = routing.indexOf(currentNode);
        List<Integer> nextNodes = new ArrayList<Integer>();
        for (int i = currentIndex + 1; i < routing.size();i++) {
            nextNodes.add(routing.get(i));
        }
        return nextNodes;
    }
    
    public int getNextNode(int currentNode) {
        int currentIndex = routing.indexOf(currentNode);
        return routing.get(currentIndex + 1);
    }
    
    public int getPreviousNode(int currentNode) {
        int currentIndex = routing.indexOf(currentNode);
        return routing.get(currentIndex - 1);
    }
    
    public int findIndex(int address) {
        return routing.indexOf(address);
    }
    
    public List<Integer> getRouting() {
        return this.routing;
    }
    
    public int getSrcAddress() {
        return this.srcAddress;
    }
    
    public int getDesAddress() {
        return this.desAddress;
    }
    
    public boolean containNode(int address) {
        return address == srcAddress || routing.contains(address) || address == desAddress;
    }
    
    public void setDesAddress(int desAddress) {
        this.desAddress = desAddress;
    }
    
    @Override
    public String toString() {
        String routing = "SOURCE: " + srcAddress + " | DESTINATION: " + desAddress + " | routing";
        for (Integer node: this.routing) {
            routing += " - " + node;
        }
        return routing;
    }
}
