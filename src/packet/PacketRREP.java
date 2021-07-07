package packet;

import router.Routing;

public class PacketRREP implements Packet {
    private int srcAddress;
    private int desAddress;
    private int UID;
    private Routing path;
    
    public PacketRREP(int srcAddress, int desAddress, int UID, Routing path) {
        this.srcAddress = srcAddress;
        this.desAddress = desAddress;
        this.UID = UID;
        this.path = path;
    }
    
    public int getSrcAddress() {
        return srcAddress;
    }
    public int getDesAddress() {
        return desAddress;
    }
    public int getUID() {
        return UID;
    }
    public Routing getPath() {
        return path;
    }
}
