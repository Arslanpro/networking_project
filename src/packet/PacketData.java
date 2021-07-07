package packet;

public class PacketData implements Packet {
    private int srcAddress;
    private int desAddress;
    private int UID;
    private String message;
    private boolean finPacket;
    private int nextNode;
    
    public PacketData(int srcAddress, int desAddress, int UID, String message, boolean isFinPacket) { 
        this.srcAddress = srcAddress;
        this.desAddress = desAddress;
        this.UID = UID;
        this.message = message;
        this.finPacket = isFinPacket;
    }
    
    public PacketData(int srcAddress, int desAddress, int UID, String message, boolean isFinPacket, int nextNode) { 
        this(srcAddress,desAddress,UID,message,isFinPacket);
        this.nextNode = nextNode;
    }
    
    public void setNextNode(int nextNode) {
        this.nextNode = nextNode;
    }
    
    public int getNextNode() {
        return this.nextNode;
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
    
    public String getMessage() {
        return this.message;
    }
    
    public boolean isFinPacket() {
        return this.finPacket;
    }
    
    @Override
    public String toString() {
        return "FROM " + this.srcAddress + " TO " + this.desAddress + ": " + this.message;
    }
}