package router;

import java.util.ArrayList;
import java.util.List;

public class RoutingTable {
    private List<Routing> routingList;
    private int myAddress;
    
    public RoutingTable(int myAddress) {
        routingList = new ArrayList<Routing>();
        this.myAddress = myAddress;
    }
    
    //go through the list to indicate whether we can go to the destination address.
    public boolean canGoTo(int address) {
        for (Routing routing: routingList) {
            if (routing.getDesAddress() == address) {
                return true;
            }
        }
        return false;
    }

    //Add missing node to the list
    public void updateBreakNode(int breakNode, int desAddress) {
        List<Integer> missingNodes = new ArrayList<Integer>();
        for (Routing routing: routingList) {
            if (routing.getDesAddress() == desAddress) {
                for (Integer missingNode: routing.getNextNodes(breakNode)) {
                    missingNodes.add(missingNode);
                }
            }
        }
        
        for (int missingNode: missingNodes) {
            removeMissingNode(missingNode);
        }
    }
    
    // if we find the missing node remove it from the list
    private void removeMissingNode(int missingNode) {
        for (Routing routing: routingList) {
            if (routing.getDesAddress() == missingNode) {
                routingList.remove(routing);
                break;
            }
        }
    }

    // if destination address and source address have added in the list, add the current node to the next node list.
    public int getNextNode(int srcAddress, int desAddress, int currentNode) {
        int result = -1;
        for (Routing routing: routingList) {
            if (routing.getDesAddress() == desAddress && routing.getSrcAddress() == srcAddress) {
                result = routing.getNextNode(currentNode);
                break;
            }
        }
        return result;
    }

    public int getPreviousNode(int srcAddress, int desAddress, int currentNode) {
        for (Routing routing: routingList) {
            if (routing.getSrcAddress() == myAddress && routing.getDesAddress() == srcAddress) {
                return routing.getRouting().get(1);
            }
        }
        
        return -1;
    }
    
    // update the routing table if there is new address.
    public void updateTable(Routing routing) {
        int myIndexInrouting = routing.findIndex(myAddress);
        if (myIndexInrouting == - 1) {
            return;
        }
        for (int i = 0;i < myIndexInrouting;i++) {
            if (canGoTo(routing.getRouting().get(i))) {
                continue;
            }
            Routing newrouting = new Routing(myAddress,routing.getRouting().get(i));
            for (int j = myIndexInrouting - 1;j >= i;j--) {
                newrouting.addNode(routing.getRouting().get(j));
            }
            routingList.add(newrouting);
        }
        for (int i = myIndexInrouting+1;i < routing.getRouting().size();i++) {
            if (canGoTo(routing.getRouting().get(i))) {
                continue;
            }
            Routing newRouting = new Routing(myAddress,routing.getRouting().get(i));
            for (int j = myIndexInrouting + 1;j <= i;j++) {
                newRouting.addNode(routing.getRouting().get(j));
            }
            routingList.add(newRouting);
        }
    }
    
    @Override
    public String toString() {
        return "CURRENT TABLE: " + routingList;
    }
}
