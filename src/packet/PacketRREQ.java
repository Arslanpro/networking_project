package packet;

import router.Routing;

public class PacketRREQ implements Packet {
    private int srcAddress;
    private int desAddress;
    private int broadcast_id;
    private Routing route;
    
    public PacketRREQ(int srcAddress, int desAddress, int broadcast_id, Routing route) {
        this.srcAddress = srcAddress;
        this.desAddress = desAddress;
        this.broadcast_id = broadcast_id;
        this.route = route;
    }
    
    public int getSrcAddress() {
        return srcAddress;
    }
    public int getDesAddress() {
        return desAddress;
    }
    public int getBroadcast_id() {
        return broadcast_id;
    }
    public Routing getRoute() {
        return route;
    }
}