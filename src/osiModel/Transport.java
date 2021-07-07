package osiModel;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import client.Client;
import node.Node;
import packet.Packet;
import packet.PacketData;
import packet.PacketUACK;

public class Transport {
    private final static boolean IS_FIN = true;
    private final static boolean IS_NOT_FIN = false;
    
    private Client client;
    private Application upperLayer;
    private Network lowerLayer;
    private Integer[] UID;
    private TimeoutThread[][] timeoutThreadHolder;
    private PacketData[][] savedSendingPacketData;
    private Map<Integer,List<PacketData>> savedReceivingPacketData;
    private Map<Integer, List<Integer>> processedDataMap;
    private Map<Integer,List<PacketUACK>> processedUACKMap;
    
    public Transport(Client client) {
        this.client = client;
        this.UID = new Integer[4];
        Arrays.fill(this.UID, 0);
        this.timeoutThreadHolder = new TimeoutThread[4][8];
        this.savedSendingPacketData = new PacketData[4][8];
        this.savedReceivingPacketData = new HashMap<Integer,List<PacketData>>();
        this.processedUACKMap = new HashMap<Integer,List<PacketUACK>>();
        this.processedDataMap = new HashMap<Integer,List<Integer>>();
    }
    
    public void setUpperLayer(Application applicationLayer) {
        this.upperLayer = applicationLayer;
    }
    
    public void setLowerLayer(Network networkLayer) {
        this.lowerLayer = networkLayer;
    }
    
    //combine segments and send String message to application layer
    public synchronized void receiveFromLowerLayer(Packet packet) {
        if (packet instanceof PacketData) {
            PacketData thisPacket = (PacketData) packet;
            if (!processedDataMap.containsKey(thisPacket.getSrcAddress()) 
                    || !processedDataMap.get(thisPacket.getSrcAddress()).contains(thisPacket.getUID())) {
                saveToReceivingPacketData(thisPacket);
                putToProcessedDataMap(thisPacket);
                if (thisPacket.isFinPacket()) {
                    List<PacketData> savedReceivedPacketDataList = this.savedReceivingPacketData.get(thisPacket.getSrcAddress());
                    String textMessage = combineReceivedPacketDataToTextMessage(savedReceivedPacketDataList);
                    this.savedReceivingPacketData.remove(thisPacket.getSrcAddress());
                    upperLayer.receiveFromLowerLayer(textMessage, thisPacket.getSrcAddress());
                }
            }

            PacketUACK UACKPacket = new PacketUACK(client.getAddress(),thisPacket.getSrcAddress(),thisPacket.getUID());
            lowerLayer.receiveFromUpperLayer(UACKPacket);
        } else if (packet instanceof PacketUACK) {
            PacketUACK thisPacket = (PacketUACK) packet;
            if (processedUACKMap.containsKey(thisPacket.getSrcAddress())
                    && processedUACKMap.get(thisPacket.getSrcAddress()).contains(thisPacket)) {
                return;
            } else {
                putToProcessedUACKMap(thisPacket);
            }
            TimeoutThread timeoutThread = this.timeoutThreadHolder[thisPacket.getSrcAddress()][thisPacket.getUID()];
            if (timeoutThread != null) {
                PacketData sentPacketData = timeoutThread.getPacket();
                timeoutThread.stopTimer();
                this.savedSendingPacketData[sentPacketData.getDesAddress()][sentPacketData.getUID()] = null;
                if (sentPacketData.isFinPacket()) {
                    upperLayer.receiveFromLowerLayer("Message to " + Node.getNode(sentPacketData.getDesAddress()).getNodeColor() +
                            " sent successfully", Node.SYSTEM_IP);
                } else {
                    int nextUID = getNextUID(sentPacketData.getUID());
                    PacketData nextPacketData = this.savedSendingPacketData[sentPacketData.getDesAddress()][nextUID];
                    TimeoutThread nextTimeoutThread = new TimeoutThread(this,nextPacketData);
                    timeoutThreadHolder[nextPacketData.getDesAddress()][nextPacketData.getUID()] = nextTimeoutThread;
                    updateUID(nextPacketData.getDesAddress());
                    nextTimeoutThread.start();
                }
            }
        } 
    }
    
    
    public synchronized void receiveFromUpperLayer(String textMessage, int receiverAddress) {
        if (receiverAddress == Node.GROUP_CHAT) {
            sendGroupChatMessage(textMessage);
        } else {
            sendPrivateMessage(textMessage, receiverAddress);
        }
    }
    
    private void sendPrivateMessage(String textMessage, int receiverAddress) {
      new SingleMessageThread(this,textMessage,receiverAddress).start();
    }
    
    public int getNextUID(int currentUID) {
        if (currentUID == 7) {
            return 0;
        }
        return currentUID+1;
    }
    
    public int getNewUID(int desAddress) {
        updateUID(desAddress);
        return this.UID[desAddress];
    }
    
    private void sendGroupChatMessage(String message) {
        for (Integer NodeIP: Node.getOtherNodesIPList(client.getAddress())) {
            this.sendPrivateMessage(message, NodeIP);
        }
    }
    
    private String combineReceivedPacketDataToTextMessage(List<PacketData> packetDataList) {
        String result = "";
        for (PacketData packet: packetDataList) {
            result += packet.getMessage();
        }
        return result;
    }
    
    private void putToProcessedDataMap(PacketData packet) {
        List<Integer> processedList;
        if (processedDataMap.containsKey(packet.getSrcAddress())) {
            processedList = processedDataMap.get(packet.getSrcAddress());
        } else {
            processedList = new ArrayList<Integer>();
        }
        processedList.add(packet.getUID());
        if (processedList.size() >= 3) {
            processedList.remove(0);
        }
        processedDataMap.put(packet.getSrcAddress(), processedList);
    }
    
    private void putToProcessedUACKMap(PacketUACK packet) {
        List<PacketUACK> processedList;
        if (this.processedUACKMap.containsKey(packet.getSrcAddress())) {
            processedList = this.processedUACKMap.get(packet.getSrcAddress());
        } else {
            processedList = new ArrayList<PacketUACK>();
        }
        processedList.add(packet);
        if (processedList.size() >= 3) {
            processedList.remove(0);
        }
        this.processedUACKMap.put(packet.getSrcAddress(), processedList);
    }
    
    private void saveToReceivingPacketData(PacketData packet) {
        List<PacketData> packetDataList;
        if (this.savedReceivingPacketData.containsKey(packet.getSrcAddress())) {
            packetDataList = this.savedReceivingPacketData.get(packet.getSrcAddress());
        } else {
            packetDataList = new ArrayList<PacketData>();
        }
        packetDataList.add(packet);
        this.savedReceivingPacketData.put(packet.getSrcAddress(),packetDataList);
    }
    
    //segmentation, put segments in the two dimension array,  creating packet
    private void saveMessageToSendingArray(String textMessage, int receiverAddress) {
        int nextUID = UID[receiverAddress];
        while (textMessage.length() > 30) {
            String textPart = textMessage.substring(0, 30);
            PacketData packet = new PacketData(client.getAddress(),receiverAddress,nextUID,textPart,IS_NOT_FIN);
            this.savedSendingPacketData[receiverAddress][nextUID] = packet;
            nextUID = getNextUID(nextUID);
            textMessage = textMessage.substring(30);
        }
        PacketData finalPacket = new PacketData(client.getAddress(),receiverAddress,nextUID,textMessage,IS_FIN);
        this.savedSendingPacketData[receiverAddress][nextUID] = finalPacket;
    }
    
    private TimeoutThread[] getTimeoutThreadHolder(int receiverAddress) {
        return this.timeoutThreadHolder[receiverAddress];
    }
    
    private void updateUID(int desAddress) {
        if (this.UID[desAddress] == 7) {
            this.UID[desAddress] = 0;
        } else {
            this.UID[desAddress] = this.UID[desAddress] + 1;
        }
    }
    
    private int getUID(int receiverAddress) {
        return this.UID[receiverAddress];
    }
    
    private class SingleMessageThread extends Thread {
        private String textMessage;
        private int receiverAddress;
        private Transport transportLayer;
        
        private SingleMessageThread(Transport transportLayer, String textMessage, int receiverAddress) {
            this.transportLayer = transportLayer;
            this.textMessage = textMessage;
            this.receiverAddress = receiverAddress;
        }
        
        @Override
        public void run() {
            if (lowerLayer.candSendTo(receiverAddress)) {
                updateUID(receiverAddress);
                saveMessageToSendingArray(textMessage,receiverAddress); 
                PacketData packet = savedSendingPacketData[receiverAddress][UID[receiverAddress]];
                TimeoutThread timeoutThread = new TimeoutThread(transportLayer,packet);
                timeoutThreadHolder[packet.getDesAddress()][packet.getUID()] = timeoutThread;
                if (timeoutThread != null) {
                    timeoutThread.start();
                }
            } else {
                upperLayer.receiveFromLowerLayer( "Cannot send the message to " 
            + Node.getNode(receiverAddress).getNodeColor(),Node.SYSTEM_IP);
            }
        }
    }
    
    private class TimeoutThread extends Thread {
        private PacketData packet;
        private Transport transportLayer;
        
        private TimeoutThread(Transport transportLayer, PacketData packet) {
            this.setName("TIMEOUT-THREAD-TRANSPORT LAYER");
            this.packet = (PacketData) packet;
            this.transportLayer = transportLayer;
        }
        
        private PacketData getPacket() {
            return this.packet;
        }
        
        private void stopTimer() {
            this.interrupt();
        }
        
        @Override
        public void run() {
            int count = 0;
            while (true) {
                try {
                    if (transportLayer.getUID(packet.getDesAddress()) == packet.getUID()) {
                        lowerLayer.receiveFromUpperLayer(packet);
                    } else {
                        break;
                    }
                    sleep(20000);  // waiting for 20s
                    count++;
                    if (count == 3) {
                        upperLayer.receiveFromLowerLayer( "Cannot send the message to " 
                                + Node.getNode(packet.getDesAddress()).getNodeColor(),Node.SYSTEM_IP);
                        transportLayer.getTimeoutThreadHolder(packet.getDesAddress())[packet.getUID()] = null;
                        break;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
