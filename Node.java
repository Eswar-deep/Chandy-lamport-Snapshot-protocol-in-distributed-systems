import java.net.*;
import java.io.*;
import java.util.*;

public class Node {
    
    public int nodeID;
    //from config file
    public int totalNodes; 
    public String[] hostnames;
    public int[] portnumbers;
    public int minPerActive;
    public int maxPerActive;
    public int minSendDelay;
    public int maxNumber;
    public int snapshotDelay; 
    public List<Integer> neighbors = new ArrayList<>();


    public int messagesSent=0;
    public boolean activity;
    public Random random=new Random();
    public ServerSocket serverSocket;   
    public Map<Integer, Socket> connections= new HashMap<>(); 
    public boolean running;
    public Set<Integer> markersReceived = new HashSet<>();

    //part 1.3
    public int[] vectorClock;

    //part 1.2
    public boolean snapshotInProgress = false;
    public int currentSnapshotId = 0;
    public Map<Integer, List<String>> channelStates = new HashMap<>();
    public String nodeState = "";     

    //part 1.4
    public boolean terminationComplete = false;
    public int messagesReceived=0;

    public String configFileName;
    public List<int[]> snapshotVectorClocks = new ArrayList<>();
    public boolean snapshotTaken = false;

    // snapshot stuff
    public Integer snapshotParent = null;
    public Set<Integer> snapshotChildren = new HashSet<>();
    public Set<Integer> waitingForReports = new HashSet<>();
    public boolean wasActiveWhenRecorded = false;
    public boolean channelsWereEmpty = true;
    public boolean allNodesInactive = true;
    public boolean allChannelsEmpty = true;
    public Map<Integer, int[]> collectedVCs = new HashMap<>();
    public boolean vcConsistent = true;
    public long lastPassiveAt = 0L;

    //part 1.1
    public Node(int nodeID, String configFile) throws Exception{
        this.nodeID = nodeID;
        this.configFileName = configFile;
               
        parseConfig(configFile);
        if(this.nodeID == 0) {
            this.activity = true;
        } else {
            this.activity = false;
        }   
    }
    public void parseConfig(String filename) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        int validIndex = 0;
        while ((line = br.readLine()) != null) {
            int Indexhash = line.indexOf('#');
            if (Indexhash != -1) {
                line = line.substring(0, Indexhash).trim();
            }
            if (line.isEmpty()) {
                continue;
            }
            if (!line.matches("^\\s*\\d+.*$")) {
                continue;
            }
            String[] tokens = line.trim().split("\\s+");
            if (validIndex == 0) {
                totalNodes = Integer.parseInt(tokens[0]);
                minPerActive = Integer.parseInt(tokens[1]);
                maxPerActive = Integer.parseInt(tokens[2]);
                minSendDelay = Integer.parseInt(tokens[3]);
                snapshotDelay = Integer.parseInt(tokens[4]);
                maxNumber = Integer.parseInt(tokens[5]);
                hostnames = new String[totalNodes];
                portnumbers = new int[totalNodes];
                vectorClock = new int[totalNodes];
            } else if (validIndex <= totalNodes) {
                int nodeId = Integer.parseInt(tokens[0]);
                hostnames[nodeId] = tokens[1];
                portnumbers[nodeId] = Integer.parseInt(tokens[2]);
            } else {
                int nodeofThisLine = validIndex - totalNodes - 1;
                if (nodeofThisLine == this.nodeID) {
                    // tokens[0] is the node id; skip it when collecting neighbors
                    for (int t = 1; t < tokens.length; t++) {
                        this.neighbors.add(Integer.parseInt(tokens[t]));
                    }
                }
            }
            validIndex++;
        }
        br.close();
    }

    public void setupConnections() throws Exception {

        serverSocket = new ServerSocket(portnumbers[this.nodeID]);

        connectionsAccept();

        int neighborconnections=0;
        for (int neighborID : neighbors) {
        if (neighborID < this.nodeID) {
            neighborconnections++;
        }
    }
        while (connections.size() < neighborconnections) {
            for (int neighborID : neighbors) {
            if (neighborID < this.nodeID && !connections.containsKey(neighborID)) {
                tryConnect(neighborID);
            }
        }
             if(connections.size() < neighborconnections) {
                Thread.sleep(1000);
            }
        }
        while (connections.size() < neighbors.size()) {
            Thread.sleep(1000);
            System.out.println("Node " + nodeID + " waiting for connections. Current: " + connections.size() + "/" + neighbors.size());
        }
    }

    public void connectionsAccept() {
        running = true;
        Thread acceptThread = new Thread(() -> {
            try {
                while (running) {
                    Socket incomingSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream()));
                    String line = in.readLine();
                    int incomingNodeID = Integer.parseInt(line.trim());
                    connections.put(incomingNodeID, incomingSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        acceptThread.start();
    }

    public boolean tryConnect(int neighborID) {
    try {
        if (connections.containsKey(neighborID)) {
            return true;
        }
        
        Socket socket = new Socket(hostnames[neighborID], portnumbers[neighborID]);
        
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(this.nodeID);
        
        connections.put(neighborID, socket);
        
        return true;
        
    } catch (Exception e) {
        return false;
    }
}

    public void sendMessages() throws Exception{
        if(messagesSent >= maxNumber) {
            activity = false;
            lastPassiveAt = System.currentTimeMillis();
            return;
        }
        int messagesToSend = random.nextInt(maxPerActive - minPerActive + 1) + minPerActive;

        for(int i = 0; i < messagesToSend && messagesSent < maxNumber; i++){
            sendMessagesToRandomNeighbour();
            messagesSent++;
            System.out.println("Node " + nodeID + " sent message #" + messagesSent + " (max=" + maxNumber + ")");
            if (i < messagesToSend - 1 && messagesSent < maxNumber) {
            Thread.sleep(minSendDelay);
        }
        }    
        activity = false;
        lastPassiveAt = System.currentTimeMillis();
    }

    public void sendMessagesToRandomNeighbour() throws Exception {
        if(neighbors.isEmpty()) {
            return;
        }   
        
        incrementClock();

        int randomIndex = random.nextInt(neighbors.size());
        int randomNeighborID = neighbors.get(randomIndex);
        Socket socket = connections.get(randomNeighborID);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        String message= "Message from node " + this.nodeID + "|CLOCK:" + clockToString();
        out.println(message);
    }

    public void startMessageListener(){
        for(Map.Entry<Integer, Socket> entry : connections.entrySet()){
            int neighborID = entry.getKey();
            Socket socket = entry.getValue();
            Thread messageThread = new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message;
                    while ((message = in.readLine()) != null) {
                        handleIncomingMessage(message, neighborID);
                    }
                } catch (Exception e) {
                    if(running){
                        System.err.println("Error in listening from neighbor " + neighborID + " at " + this.nodeID + ": " + e.getMessage());
                    }
                }
            });
            messageThread.setDaemon(true);
            messageThread.start();
        }
    }
    public void handleMapMessage(String message, int senderID){

        if(snapshotInProgress){
            recordChannelMessage(senderID, message);
        }   

        String[] parts = message.split("\\|CLOCK:");
        if (parts.length < 2) {
            if(!activity && messagesSent < maxNumber && !terminationComplete){
                activity = true;
                System.out.println("Node " + nodeID + " became ACTIVE (messages sent: " + messagesSent + "/" + maxNumber + ")");
            }
            return;
        }
        String clockString = parts[1];

        int[] receivedClock = stringToClock(clockString); 
        updateVectorClock(receivedClock);
        messagesReceived++;
        
        if(!activity && messagesSent < maxNumber && !terminationComplete){
            activity = true;
            System.out.println("Node " + nodeID + " became ACTIVE (messages sent: " + messagesSent + "/" + maxNumber + ")");
        }
    }

//// part 1.3
    public void incrementClock() {
        vectorClock[this.nodeID]++;
    }
    public void updateVectorClock(int[] receivedClock) {
        for (int i = 0; i < vectorClock.length; i++) {
            vectorClock[i] = Math.max(vectorClock[i], receivedClock[i]);
        }
        vectorClock[this.nodeID]++;
    }
    public String clockToString() {
        return Arrays.toString(vectorClock);
    }
    public int[] stringToClock(String clockString) {
        String[] parts = clockString.replace("[", "").replace("]", "").split(", ");
        int[] clock = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            clock[i] = Integer.parseInt(parts[i]);
        }
        return clock;
    }


//// part 1.2

    public void beginSnapshot() throws IOException {
        currentSnapshotId++;
        snapshotInProgress = true;
        markersReceived.clear();
        channelStates.clear();
        snapshotParent = (nodeID == 0 ? null : snapshotParent);
        snapshotChildren.clear();
        waitingForReports.clear();
        if (nodeID == 0) {
            waitingForReports.addAll(neighbors);
        }
        allNodesInactive = true;
        allChannelsEmpty = true;
        collectedVCs.clear();
        recordNodeState();

        for (int neighborID : neighbors) {
        channelStates.put(neighborID, new ArrayList<>());
        }

        sendMarkerToAllNeighbors();
    }

    public void recordNodeState() {
        nodeState = "Node " + nodeID + " State: " +"vectorClock=" + clockToString() + ", messagesSent=" + messagesSent + ", activity=" + activity + ", snapshotId=" + currentSnapshotId;
        
        int[] clockCopy = vectorClock.clone();
        snapshotVectorClocks.add(clockCopy);
        if (nodeID == 0) snapshotTaken = true;
        wasActiveWhenRecorded = activity;
    }

    public void sendMarkerToAllNeighbors() throws IOException {
        for (int neighborID : neighbors) {
            Socket socket = connections.get(neighborID);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String markerMessage = "MARKER|SNAPSHOT_ID:" + currentSnapshotId;
            out.println(markerMessage);
        }
    }

    public void handleMarkerMessage(String markerString, int senderID) throws IOException {
        String[] parts = markerString.split("\\|SNAPSHOT_ID:");
        int snapshotId = Integer.parseInt(parts[1]);

        if (!snapshotInProgress) {
            snapshotInProgress = true;
            currentSnapshotId = snapshotId;
            recordNodeState();

            for (int neighborID : neighbors) {
                channelStates.put(neighborID, new ArrayList<>());
            }
            channelStates.get(senderID).clear();
            snapshotParent = senderID;
            snapshotChildren.clear();
            waitingForReports.clear();
            allNodesInactive = true;
            allChannelsEmpty = true;
            collectedVCs.clear();
            markersReceived.add(senderID);

            sendMarkerToAllNeighbors();
            sendSnapshotChildAck();
        } else {
            markersReceived.add(senderID);
            checkSnapshotComplete();
        }
    }

    

    public void recordChannelMessage(int senderID, String message) {
        if (snapshotInProgress && channelStates.containsKey(senderID) && !markersReceived.contains(senderID)) {
            channelStates.get(senderID).add(message);  
            System.out.println("Recorded message on channel from " + senderID + ": " + message);
        }
    }
    
    public void checkSnapshotComplete() {
    if (markersReceived.size() == neighbors.size()) {
        printSnapshotResults();
        boolean channelsEmpty = true;
        for (Map.Entry<Integer, List<String>> e : channelStates.entrySet()) {
            if (!e.getValue().isEmpty()) {
                channelsEmpty = false;
                break;
            }
        }
        channelsWereEmpty = channelsEmpty;
        allNodesInactive = allNodesInactive && !wasActiveWhenRecorded;
        allChannelsEmpty = allChannelsEmpty && channelsWereEmpty;
        collectedVCs.put(nodeID, snapshotVectorClocks.get(snapshotVectorClocks.size()-1));
        if (waitingForReports.isEmpty()) {
            try {
                sendSnapshotReportUpwards();
            } catch (IOException ex) {
                System.err.println("Error sending report: " + ex.getMessage());
            }
        }
    }
}
    
    public void printSnapshotResults() {
    System.out.println("Snapshop " + currentSnapshotId +"recording complete");
    System.out.println("Node State: " + nodeState);
    
    for (Map.Entry<Integer, List<String>> entry : channelStates.entrySet()) {
        int neighborID = entry.getKey();
        List<String> messages = entry.getValue();
        System.out.println("Channel from " + neighborID + ": " + messages);
    }
}

    public void writeOutputFile() throws IOException {
    
        String configName = configFileName.replace(".txt", "");
        String outputFileName = configName + "-" + nodeID + ".out";
        
        PrintWriter writer = new PrintWriter(new FileWriter(outputFileName));
        
        // Write each snapshot's vector clock on a separate line
        for (int[] vectorClock : snapshotVectorClocks) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < vectorClock.length; i++) {
                line.append(vectorClock[i]);
                if (i < vectorClock.length - 1) {
                    line.append(" ");
                }
            }
            writer.println(line.toString());
        }
        
        writer.close();
        System.out.println("Node " + nodeID + " wrote output file: " + outputFileName);
    }

    public String serializeVCMap(Map<Integer, int[]> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<Integer,int[]> e : map.entrySet()) {
            if (!first) sb.append(";");
            first = false;
            sb.append(e.getKey()).append(":").append(Arrays.toString(e.getValue()));
        }
        return sb.toString();
    }

    public Map<Integer,int[]> deserializeVCMap(String s) {
        Map<Integer,int[]> m = new HashMap<>();
        if (s == null || s.isEmpty()) return m;
        String[] parts = s.split(";");
        for (String p : parts) {
            String[] kv = p.split(":", 2);
            if (kv.length != 2) continue;
            int id = Integer.parseInt(kv[0]);
            int[] vc = stringToClock(kv[1]);
            m.put(id, vc);
        }
        return m;
    }

    public void sendSnapshotChildAck() throws IOException {
        if (snapshotParent == null) return;
        Socket socket = connections.get(snapshotParent);
        if (socket == null) return;
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        String msg = "SNAPSHOT_CHILD|ID:" + currentSnapshotId + "|FROM:" + nodeID;
        out.println(msg);
    }

    public boolean verifySnapshotConsistency(Map<Integer, int[]> vcMap) {
        boolean ok = true;
        for (Map.Entry<Integer,int[]> ei : vcMap.entrySet()) {
            int i = ei.getKey();
            int[] vci = ei.getValue();
            for (Map.Entry<Integer,int[]> ej : vcMap.entrySet()) {
                int j = ej.getKey();
                int[] vcj = ej.getValue();
                if (i == j) continue;
                if (j >= vci.length || j >= vcj.length) continue;
                if (vci[j] > vcj[j]) {
                    ok = false;
                }
            }
        }
        return ok;
    }

    public void sendSnapshotReportUpwards() throws IOException {
        if (snapshotParent == null) {
            boolean haveAllVCs = collectedVCs.size() == totalNodes;
            System.out.println("Snapshot #" + currentSnapshotId + " converge-cast at root: allInactive=" + allNodesInactive + 
                               ", allChannelsEmpty=" + allChannelsEmpty + ", haveAllVCs=" + haveAllVCs);
            if (haveAllVCs) {
                vcConsistent = verifySnapshotConsistency(collectedVCs);
                System.out.println("Vector-clock consistency check: " + (vcConsistent ? "PASS" : "FAIL"));
            } else {
                vcConsistent = false;
            }
            if (haveAllVCs && allNodesInactive && allChannelsEmpty) {
                if (!snapshotTaken) {
                    System.out.println("Node 0: Forcing a snapshot before termination");
                    beginSnapshot();
                } else {
                    terminationComplete = true;
                    String separator = "==================================================";
                    System.out.println("\n" + separator);
                    System.out.println("*** TERMINATION DETECTED BY NODE 0 (via snapshot) ***");
                    System.out.println("All nodes inactive and channels empty in snapshot " + currentSnapshotId);
                    System.out.println("Snapshot VC consistency: " + (vcConsistent ? "PASS" : "FAIL"));
                    System.out.println("Time: " + new java.util.Date());
                    System.out.println("=== DISTRIBUTED SYSTEM WILL NOW TERMINATE ===");
                    System.out.println(separator + "\n");
                    broadcastTermination();
                }
            }
            snapshotInProgress = false;
            markersReceived.clear();
            channelStates.clear();
            snapshotChildren.clear();
            waitingForReports.clear();
            collectedVCs.clear();
            return;
        }
        Socket socket = connections.get(snapshotParent);
        if (socket == null) return;
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        String payload = "SNAPSHOT_REPORT|ID:" + currentSnapshotId + "|FROM:" + nodeID +
                         "|ALL_INACTIVE:" + allNodesInactive + "|ALL_CHANNELS_EMPTY:" + allChannelsEmpty +
                         "|VCMAP:" + serializeVCMap(collectedVCs);
        out.println(payload);
        snapshotInProgress = false;
        markersReceived.clear();
        channelStates.clear();
        snapshotChildren.clear();
        waitingForReports.clear();
        collectedVCs.clear();
    }

    public void handleSnapshotReport(String message, int senderID) throws IOException {
        String[] parts = message.split("\\|");
        int snapId = Integer.parseInt(parts[1].split(":")[1]);
        if (snapId != currentSnapshotId) return;
        boolean childAllInactive = Boolean.parseBoolean(parts[3].split(":")[1]);
        boolean childAllChannelsEmpty = Boolean.parseBoolean(parts[4].split(":")[1]);
        String vcMapStr = parts.length > 5 && parts[5].startsWith("VCMAP:") ? parts[5].substring("VCMAP:".length()) : "";
        Map<Integer,int[]> childMap = deserializeVCMap(vcMapStr);
        allNodesInactive = allNodesInactive && childAllInactive;
        allChannelsEmpty = allChannelsEmpty && childAllChannelsEmpty;
        collectedVCs.putAll(childMap);
        if (waitingForReports.contains(senderID)) waitingForReports.remove(senderID);
        if (markersReceived.size() == neighbors.size() && waitingForReports.isEmpty()) {
            sendSnapshotReportUpwards();
        }
    }

    public void handleSnapshotChild(String message, int senderID) {
        String[] parts = message.split("\\|");
        int snapId = Integer.parseInt(parts[1].split(":")[1]);
        if (snapId != currentSnapshotId) return;
        snapshotChildren.add(senderID);
        waitingForReports.add(senderID);
    }

    ///1.4
    
    public void broadcastTermination() throws IOException {
        for (int neighborID : neighbors) {
            Socket socket = connections.get(neighborID);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("TERMINATE");
        }
    }
    
    public void handleTerminateMessage(int senderID) throws IOException {
        if (terminationComplete) return;
        terminationComplete = true;
        System.out.println("Node " + nodeID + " received termination notification from Node " + senderID);
        for (int neighborID : neighbors) {
            if (neighborID == senderID) continue;
            Socket socket = connections.get(neighborID);
            if (socket == null) continue;
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("TERMINATE");
        }
    }
    
    public void handleIncomingMessage(String message, int senderID) throws IOException {
        if (message.startsWith("MARKER|")) {
            handleMarkerMessage(message, senderID);
        } else if (message.contains("|CLOCK:")) {
            handleMapMessage(message, senderID);
        } else if (message.startsWith("SNAPSHOT_REPORT|")) {
            handleSnapshotReport(message, senderID);
        } else if (message.startsWith("SNAPSHOT_CHILD|")) {
            handleSnapshotChild(message, senderID);
        } else if (message.equals("TERMINATE")) {
            handleTerminateMessage(senderID);
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Node <nodeId> <configPath>");
            return;
        }
        Node node=new Node(Integer.parseInt(args[0]), args[1]);

        node.setupConnections();
        node.startMessageListener();

        long lastSnapshotTime = System.currentTimeMillis();

        while (node.running && !node.terminationComplete) { 
            if (node.activity) {
                node.sendMessages();
            }
            if (node.nodeID == 0 && !node.snapshotInProgress && !node.activity) {
                long currentTime = System.currentTimeMillis();
                if (node.lastPassiveAt > 0 &&
                    (currentTime - node.lastPassiveAt) >= node.snapshotDelay &&
                    (currentTime - lastSnapshotTime) >= node.snapshotDelay) {
                    node.beginSnapshot();
                    lastSnapshotTime = currentTime;
                }
            }

            Thread.sleep(100);
        }
        
        if (node.terminationComplete) {
            System.out.println("Node " + node.nodeID + " exiting due to termination detection");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        node.writeOutputFile();
    }

}