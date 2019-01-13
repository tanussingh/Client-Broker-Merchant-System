import java.io.*;

public class Parser {
    public static Nodes[] parse (String PATH) {
        File file = new File(PATH);
        BufferedReader bufferedReader = null;
        String line;
        int index;
        boolean empty;
        boolean found = false;
        int numNodes = 0;

        try {
            //open file
            FileReader fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            
            //get numNodes
            while (!found) {
                line = bufferedReader.readLine();
                empty = false;
                //format line to just have what we want
                if (line.contains("#")) {
                    index = line.indexOf("#");
                    line = line.substring(0, index);
                }
                if (line.isEmpty()) {
                    empty = true;
                }
                line = line.trim();
                //create nodes
                if (!empty) {
                    //parse info
                    numNodes = Integer.parseInt(line);
                    found = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //populate node information
        Nodes[] array_of_nodes = new Nodes[numNodes];
        try {
            int valid_lines = 0;
            while (valid_lines != numNodes) {
                line = bufferedReader.readLine();
                empty = false;
                //format line to just have what we want
                if (line.contains("#")) {
                    index = line.indexOf("#");
                    line = line.substring(0, index);
                }
                if (line.isEmpty()) {
                    empty = true;
                }
                line = line.trim();
                //create nodes
                if (!empty) {
                    //parse info
                    Nodes node = new Nodes();
                    index = line.indexOf(" ");
                    node.setNodeId(Integer.parseInt(line.substring(0, index)));
                    line = line.substring(index).trim();
                    index = line.indexOf(" ");
                    node.setHostName(line.substring(0, index));
                    line = line.substring(index).trim();
                    node.setPortNumber(Integer.parseInt(line));
                    
                    String[] items = new String[6];
                    items[0] = bufferedReader.readLine();
                    items[1] = bufferedReader.readLine();
                    items[2] = bufferedReader.readLine();
                    items[3] = bufferedReader.readLine();
                    items[4] = bufferedReader.readLine();
                    items[5] = bufferedReader.readLine();
                    node.setItems(items);

                    node.setKey(bufferedReader.readLine());

                    array_of_nodes[valid_lines] = node;
                    valid_lines++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return array_of_nodes;
    }
}