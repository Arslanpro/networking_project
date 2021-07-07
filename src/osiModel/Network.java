package osiModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import client.Client;
import packet.Packet;
import packet.PacketData;
import packet.PacketACK;
import packet.PacketRREP;
import packet.PacketRREQ;
import packet.PacketRRER;
import packet.PacketUACK;
import router.Routing;
import router.RoutingTable;

public class Network {
	private Client client;
	private Transport upperLayer;
	private DataLink lowerLayer;
	private RoutingTable routingTable;
	private Map<Integer, List<TimeoutThread>> timeoutThreadHolderMap;
	private Map<Integer, List<Integer>> processedUACKMap;
	private Map<Integer, List<Integer[]>> processedMACKMap;
	private Map<Integer, List<Integer>> processedRREQMap;
	private Map<Integer, List<Integer>> processedRREPMap;
	private Map<Integer, List<Integer>> processedRRERMap;

	public Network(Client client) {
		this.client = client;
		this.routingTable = new RoutingTable(client.getAddress());
		this.timeoutThreadHolderMap = new HashMap<Integer, List<TimeoutThread>>();
		processedUACKMap = new HashMap<Integer, List<Integer>>();
		processedMACKMap = new HashMap<Integer, List<Integer[]>>();
		processedRREQMap = new HashMap<Integer, List<Integer>>();
		processedRREPMap = new HashMap<Integer, List<Integer>>();
		processedRRERMap = new HashMap<Integer, List<Integer>>();
	}

	public void setUpperLayer(Transport transportLayer) {
		this.upperLayer = transportLayer;
	}

	public void setLowerLayer(DataLink linkLayer) {
		this.lowerLayer = linkLayer;
	}

	public boolean candSendTo(int receiverAddress) {
		if (routingTable.canGoTo(receiverAddress)) {
			return true;
		}
		return sendingRREQ(receiverAddress);
	}

	public synchronized void receiveFromLowerLayer(Packet packet) {
		if (packet instanceof PacketData) {
			handleReceivedPacketData((PacketData) packet);
		} else if (packet instanceof PacketACK) {
			handleReceivedPacketMACK((PacketACK) packet);
		} else if (packet instanceof PacketRREP) {
			handleReceivedPacketRREP((PacketRREP) packet);
		} else if (packet instanceof PacketRREQ) {
			handleReceivedPacketRREQ((PacketRREQ) packet);
		} else if (packet instanceof PacketRRER) {
			handleReceivedPacketRRER((PacketRRER) packet);
		} else if (packet instanceof PacketUACK) {
			handleReceivedPacketUACK((PacketUACK) packet);
		}
	}

	public synchronized void receiveFromUpperLayer(Packet packet) {
		isSavedToProcessedMap(packet);
		if (packet instanceof PacketData) {
			PacketData thisPacket = (PacketData) packet;
			int nextNode = routingTable.getNextNode(client.getAddress(), thisPacket.getDesAddress(), client.getAddress());
			thisPacket.setNextNode(nextNode);
			TimeoutThread timeoutThread = new TimeoutThread(this, thisPacket);
			this.putToTimeoutThreadHolderMap(thisPacket.getSrcAddress(), timeoutThread); 

			timeoutThread.start();
		} else if (packet instanceof PacketUACK) {
			PacketUACK thisPacket = (PacketUACK) packet;
			thisPacket.setNextNode(routingTable.getNextNode(client.getAddress(), thisPacket.getOriginalSrcAddress(),
					client.getAddress()));
			lowerLayer.receiveFromUpperLayer(thisPacket);
		}
	}

	//start routing
	private boolean sendingRREQ(int desAddress) {
		int count = 0;
		while (count < 3) {
			int UID = upperLayer.getNewUID(desAddress);
			Routing newRouting = new Routing(client.getAddress(), desAddress);
			Packet RREQ = new PacketRREQ(client.getAddress(), desAddress, UID, newRouting);

			List<Integer> processedList;
			if (processedRREQMap.containsKey(client.getAddress())) {
				processedList = processedRREQMap.get(client.getAddress());
				int oldUIDIndex = processedList.indexOf(UID);
				if (oldUIDIndex != -1) {
					processedList.remove(oldUIDIndex);
				}
			} else {
				processedList = new ArrayList<Integer>();
			}
			processedList.add(UID);
			processedRREQMap.put(client.getAddress(), processedList);

			try {
				count++;
				lowerLayer.receiveFromUpperLayer(RREQ);
				Thread.sleep(7500);  //waiting for 7.5s
				if (routingTable.canGoTo(desAddress)) {
					return true;
				}
			} catch (InterruptedException e) {
				return false;
			} 
		}
		return false;
	}

	private void handleReceivedPacketData(PacketData packet) {
		if (packet.getNextNode() != client.getAddress()) {
			return;
		}
		if (isSavedToProcessedMap(packet)) {
			return;
		}
		if (packet.getDesAddress() == client.getAddress()) {
			sendPacketMACK(packet);
			upperLayer.receiveFromLowerLayer(packet);
		} else if (packet.getNextNode() == client.getAddress()) {
			sendPacketMACK(packet);
			int nextNode = routingTable.getNextNode(client.getAddress(), packet.getDesAddress(), client.getAddress());
			if (nextNode != -1) {
				packet.setNextNode(nextNode);
				TimeoutThread timeoutThread = new TimeoutThread(this, packet);
				this.putToTimeoutThreadHolderMap(packet.getSrcAddress(), timeoutThread);
				timeoutThread.start();
			} else {
				int receiveNode = routingTable.getPreviousNode(packet.getSrcAddress(), packet.getDesAddress(),
						client.getAddress());
				PacketRRER packetRRER = new PacketRRER(packet.getSrcAddress(), packet.getDesAddress(), packet.getUID(),
						client.getAddress(), receiveNode);
				lowerLayer.receiveFromUpperLayer(packetRRER);
			}
		}

	}

	private void sendPacketMACK(PacketData packet) {
		int previousNode = routingTable.getPreviousNode(packet.getSrcAddress(), packet.getDesAddress(),
				client.getAddress());
		PacketACK packetMACK = new PacketACK(packet.getSrcAddress(), packet.getUID(), client.getAddress(),
				previousNode);
		putToProcessedMACKMap(packetMACK);
		lowerLayer.receiveFromUpperLayer(packetMACK);
	}

	private void handleReceivedPacketUACK(PacketUACK packet) {
		if (packet.getOriginalSrcAddress() == client.getAddress() && isSavedToProcessedMap(packet)) {
			return;
		}

		if (packet.getOriginalSrcAddress() == client.getAddress()) {
			upperLayer.receiveFromLowerLayer(packet);
		} else if (packet.getNextNode() == client.getAddress()) {
			packet.setNextNode(
					routingTable.getNextNode(client.getAddress(), packet.getOriginalSrcAddress(), client.getAddress()));
			lowerLayer.receiveFromUpperLayer(packet);
		}
	}

	//send ACK when message arrives destination code 
	private void handleReceivedPacketMACK(PacketACK packet) {
		if (packet.getReceiverNode() != client.getAddress()) {
			return;
		}

		if (isSavedToProcessedMap(packet)) {
			return;
		}

		if (packet.getReceiverNode() == client.getAddress()) {
			List<TimeoutThread> timeoutThreadList = this.timeoutThreadHolderMap.get(packet.getOriginalSrcAddress());
			if (timeoutThreadList != null) {
				for (int i = 0; i < timeoutThreadList.size(); i++) {
					TimeoutThread timeoutThread = timeoutThreadList.get(i);
					if (timeoutThread.getPacket().getUID() == packet.getOriginalUID()) {
						timeoutThread.stopTimer();
						timeoutThreadList.remove(i);
						timeoutThreadHolderMap.put(packet.getOriginalSrcAddress(), timeoutThreadList);
						break;
					}
				}
			}
		}
	}

	private void handleReceivedPacketRREP(PacketRREP packet) {
		if (isSavedToProcessedMap(packet)) {
			return;
		}

		Routing path = packet.getPath();
		if (path.containNode(client.getAddress())) { //confirm route back along the reverse path
			routingTable.updateTable(path);
			if (client.getAddress() != packet.getSrcAddress()) {
				lowerLayer.receiveFromUpperLayer(packet);
			}
		}
	}

	//send RREP if this client is the receiver, continue broadcast if not
	private void handleReceivedPacketRREQ(PacketRREQ packet) {
		if (isSavedToProcessedMap(packet)) {
			return;
		}

		Routing route = packet.getRoute();
		route.addNode(client.getAddress());
		if (packet.getDesAddress() == client.getAddress()) {
			routingTable.updateTable(route);
			PacketRREP replyPacket = new PacketRREP(packet.getSrcAddress(), packet.getDesAddress(), packet.getBroadcast_id(),
					route);
			putToProcessedRREPMap(replyPacket);
			lowerLayer.receiveFromUpperLayer(replyPacket);
		} else {
			PacketRREQ continueRREQPacket = new PacketRREQ(packet.getSrcAddress(), packet.getDesAddress(),
					packet.getBroadcast_id(), route);
			lowerLayer.receiveFromUpperLayer(continueRREQPacket);
		}
	}

	private void handleReceivedPacketRRER(PacketRRER packet) {
		if (packet.getReceivingNode() != client.getAddress()) {
			return;
		}
		if (isSavedToProcessedMap(packet)) {
			return;
		}

		routingTable.updateBreakNode(packet.getBreakNode(), packet.getDesAddress());
		if (packet.getSrcAddress() != client.getAddress()) {
			int previousNode = routingTable.getPreviousNode(packet.getSrcAddress(), packet.getDesAddress(),
					client.getAddress());
			packet.setReceivingNode(previousNode);
			lowerLayer.receiveFromUpperLayer(packet);
		}
	}

	private boolean isSavedToProcessedMap(Packet packet) {
		if (packet instanceof PacketRREP) {
			PacketRREP thisPacket = (PacketRREP) packet;
			if (processedRREPMap.containsKey(thisPacket.getSrcAddress())) {
				if (processedRREPMap.get(thisPacket.getSrcAddress()).contains(thisPacket.getUID())) {
					return true;
				}
			}
			putToProcessedRREPMap(thisPacket);
		} else if (packet instanceof PacketRREQ) {
			PacketRREQ thisPacket = (PacketRREQ) packet;
			if (processedRREQMap.containsKey(thisPacket.getSrcAddress())) {
				if (processedRREQMap.get(thisPacket.getSrcAddress()).contains(thisPacket.getBroadcast_id())) {
					return true;
				}
			}
			putToProcessedRREQMap(thisPacket);
		} else if (packet instanceof PacketUACK) {
			PacketUACK thisPacket = (PacketUACK) packet;
			if (processedUACKMap.containsKey(thisPacket.getOriginalSrcAddress())) {
				if (processedUACKMap.get(thisPacket.getOriginalSrcAddress()).contains(thisPacket.getUID())) {
					return true;
				}
			}
			putToProcessedUACKMap(thisPacket);
		} else if (packet instanceof PacketACK) {
			PacketACK thisPacket = (PacketACK) packet;
			if (processedMACKMap.containsKey(thisPacket.getSenderNode())) {
				Integer[] srcAndUID = new Integer[2];
				srcAndUID[0] = thisPacket.getOriginalSrcAddress();
				srcAndUID[1] = thisPacket.getOriginalUID();
				if (processedMACKMap.get(thisPacket.getSenderNode()).contains(srcAndUID)) {
					return true;
				}
			}
			putToProcessedMACKMap(thisPacket);
		} else if (packet instanceof PacketRRER) {
			PacketRRER thisPacket = (PacketRRER) packet;
			if (processedRRERMap.containsKey(thisPacket.getSrcAddress())) {
				if (processedRRERMap.get(thisPacket.getSrcAddress()).contains(thisPacket.getUID())) {
					return true;
				}
			}
			putToProcessedRRERMap(thisPacket);
		}

		return false;
	}

	private void putToTimeoutThreadHolderMap(int srcAddress, TimeoutThread timeoutThread) {
		List<TimeoutThread> timeoutThreadList;
		if (timeoutThreadHolderMap.containsKey(srcAddress)) {
			timeoutThreadList = timeoutThreadHolderMap.get(srcAddress);
		} else {
			timeoutThreadList = new ArrayList<TimeoutThread>();
		}
		timeoutThreadList.add(timeoutThread);
		timeoutThreadHolderMap.put(srcAddress, timeoutThreadList);
	}

	private void putToProcessedRREPMap(PacketRREP packet) {
		List<Integer> processedList;
		if (processedRREPMap.containsKey(packet.getSrcAddress())) {
			processedList = processedRREPMap.get(packet.getSrcAddress());
		} else {
			processedList = new ArrayList<Integer>();
		}
		processedList.add(packet.getUID());
		if (processedList.size() >= 3) {
			processedList.remove(0);
		}
		processedRREPMap.put(packet.getSrcAddress(), processedList);
	}

	private void putToProcessedRRERMap(PacketRRER packet) {
		List<Integer> processedList;
		if (processedRRERMap.containsKey(packet.getSrcAddress())) {
			processedList = processedRRERMap.get(packet.getSrcAddress());
		} else {
			processedList = new ArrayList<Integer>();
		}
		processedList.add(packet.getUID());
		if (processedList.size() >= 3) {
			processedList.remove(0);
		}
		processedRRERMap.put(packet.getSrcAddress(), processedList);
	}

	private void putToProcessedUACKMap(PacketUACK packet) {
		List<Integer> processedList;
		if (processedUACKMap.containsKey(packet.getOriginalSrcAddress())) {
			processedList = processedUACKMap.get(packet.getOriginalSrcAddress());
		} else {
			processedList = new ArrayList<Integer>();
		}
		processedList.add(packet.getUID());
		if (processedList.size() >= 3) {
			processedList.remove(0);
		}
		processedUACKMap.put(packet.getOriginalSrcAddress(), processedList);
	}

	private void putToProcessedMACKMap(PacketACK packet) {
		List<Integer[]> processedList;
		if (processedMACKMap.containsKey(packet.getSenderNode())) {
			processedList = processedMACKMap.get(packet.getSenderNode());
		} else {
			processedList = new ArrayList<Integer[]>();
		}
		Integer[] srcAndUID = new Integer[2];
		srcAndUID[0] = packet.getOriginalSrcAddress();
		srcAndUID[1] = packet.getOriginalUID();
		processedList.add(srcAndUID);
		if (processedList.size() >= 4) {
			processedList.remove(0);
		}
		processedMACKMap.put(packet.getOriginalSrcAddress(), processedList);
	}

	private void putToProcessedRREQMap(PacketRREQ packet) {
		List<Integer> processedList;
		if (processedRREQMap.containsKey(packet.getSrcAddress())) {
			processedList = processedRREQMap.get(packet.getSrcAddress());
		} else {
			processedList = new ArrayList<Integer>();
		}
		processedList.add(packet.getBroadcast_id());
		if (processedList.size() >= 3) {
			processedList.remove(0);
		}
		processedRREQMap.put(packet.getSrcAddress(), processedList);
	}

	private DataLink getLowerLayer() {
		return this.lowerLayer;
	}

	private class TimeoutThread extends Thread {
		private PacketData packet;
		private Network networkLayer;

		private TimeoutThread(Network networkLayer, Packet packet) {
			this.packet = (PacketData) packet;
			this.networkLayer = networkLayer;
		}

		private void stopTimer() {
			this.interrupt();
		}

		private PacketData getPacket() {
			return this.packet;
		}

		@Override
		public void run() {
			int count = 0;
			while (true) {
				try {
					networkLayer.getLowerLayer().receiveFromUpperLayer(packet);
					count++;
					sleep(10000); //waiting for 10s
					if (count == 3) {
						break;
					}
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
}
