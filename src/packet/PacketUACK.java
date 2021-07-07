package packet;

public class PacketUACK implements Packet {
    private int srcAddress;
    private int originalSrcAddress;
    private int UID;
    private int nextNode;
    
    public PacketUACK(int srcAddress, int originalSrcAddress, int UID) {
        this.srcAddress = srcAddress;
        this.originalSrcAddress = originalSrcAddress;
        this.UID = UID;
    }
    
    public PacketUACK(int srcAddress, int originalSrcAddress, int UID, int nextNode) {
        this(srcAddress,originalSrcAddress,UID);
        this.nextNode = nextNode;
    }
    
    public int getNextNode() {
        return this.nextNode;
    }
    
    public void setNextNode(int nextNode) {
        this.nextNode = nextNode;
    }

    public int getSrcAddress() {
        return srcAddress;
    }

    public int getOriginalSrcAddress() {
        return originalSrcAddress;
    }

    public int getUID() {
        return UID;
    }
}
