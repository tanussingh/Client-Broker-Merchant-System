public class Nodes {
    private int nodeId;
    private String hostName;
    private int portNumber;

    public Nodes(int i, String j, int k) {
        this.nodeId = i;
        this.hostName = j;
        this.portNumber = k;
    }
    
    public int getNodeId() {
        return this.nodeId;
    }

    public String getHostName() {
        return this.hostName;
    }

    public int getPortNumber() {
        return this.portNumber; 
    }
}