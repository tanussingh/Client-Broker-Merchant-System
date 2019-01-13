public class Nodes {
    private int nodeId;
    private String hostName;
    private int portNumber;
    private String[] items;
    private String keys;

    public void setNodeId(int i) {
        this.nodeId = i;
    }

    public void setHostName(String j) {
        this.hostName = j;
    }

    public void setPortNumber(int k) {
        this.portNumber = k;
    }

    public void setItems(String[] l) {
        this.items = l;
    }

    public void setKey(String i) {
        this.keys = i;
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

    public String[] getItems() {
        return this.items;
    }

    public String getKey() {
        return this.keys;
    }
}