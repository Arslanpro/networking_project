package packet;

public class PacketACK implements Packet {
    private int originalSrcAddress;
    private int originalUID;
    private int receiverNode;
    private int senderNode;
    
    public PacketACK(int originalSrcAddress, int originalUID, int senderNode ,int receiverNode) {
        this.receiverNode = receiverNode;
        this.originalSrcAddress = originalSrcAddress;
        this.originalUID = originalUID;
        this.senderNode = senderNode;
    }
    
    public void setSenderNode(int senderNode) {
        this.senderNode = senderNode;
    }
    
    public void setNextNode(int receiverNode) {
        this.receiverNode = receiverNode;
    }
    
    public int getSenderNode() {
        return this.senderNode;
    }
    
    public int getReceiverNode() {
        return this.receiverNode;
    }
    
    public int getOriginalSrcAddress() {
        return originalSrcAddress;
    }

    public int getOriginalUID() {
        return originalUID;
    }
}
