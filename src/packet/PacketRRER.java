package packet;

public class PacketRRER implements Packet{
    private int srcAddress;
    private int desAddress;
    private int UID;
    private int breakNode;
    private int receivingNode;
    
    public PacketRRER(int srcAddress, int desAddress, int UID, int breakNode) {
        this.srcAddress = srcAddress;
        this.desAddress = desAddress;
        this.UID = UID;
        this.breakNode = breakNode;
    }
    
    public PacketRRER(int srcAddress, int desAddress, int UID, int breakNode, int receivingNode) {
        this(srcAddress,desAddress,UID,breakNode);
        this.receivingNode = receivingNode;
    }
    
    public void setReceivingNode(int receivingNode) {
        this.receivingNode = receivingNode;
    }
    
    public int getReceivingNode() {
        return receivingNode;
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
    public int getBreakNode() {
        return this.breakNode;
    }
}
