import java.io.*;

public class Main {
    public static void main(String[] args) {
    	String PATH = System.getProperty("user.dir");
    	PATH = PATH + "/CS6349/Project/merchant/src/config_file.txt";
    	Nodes[] array_of_nodes = Parser.parse(PATH);
		
		//start all 5 merchants
		Merchant mer = new Merchant(array_of_nodes, Integer.parseInt(args[0]));
    }
}