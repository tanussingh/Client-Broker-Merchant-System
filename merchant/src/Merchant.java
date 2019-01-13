import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.*;
import java.util.UUID;
import java.util.Base64;
import java.util.Random;
import java.nio.file.*;
import javax.crypto.*;
import javax.crypto.spec.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Merchant extends Thread {
    //set up variables
    private Nodes[] array_of_nodes;
    private int merchantNum;
    private String products = "1. Car,2. Soap,3. iPad,4. Watch,5. Phone,6. Harddrive,200,300,400,500,600,700";
    private int[] price = new int[6];
    private String name;

    //Keys, public private will load from other files, secretket will generate as we go
    PrivateKey merchantPr;
    PublicKey merchantP;
    PublicKey brokerP;
    SecretKey brokerSym;
    SecretKey clientSym;
    
    Merchant (Nodes[] array_of_nodes, int merchantNum) {
        this.array_of_nodes = array_of_nodes;
        this.merchantNum = merchantNum;
        this.price[0] = 200;
        this.price[1] = 300;
        this.price[2] = 400;
        this.price[3] = 500;
        this.price[4] = 600;
        this.price[5] = 700;
        Thread t = new Thread(this);
        t.start();
    }

    public void run() {
        //initilize keys
        loadKeys(array_of_nodes[merchantNum].getKey());
        name = "merchant" + (merchantNum+1);

        //initialize socket and input/output stream
        ServerSocket server = null;
        Socket socket = null;
        PrintWriter pw = null;
        BufferedReader br = null;
        
        //initilize merchant host and port
        int merchantPort = this.array_of_nodes[merchantNum].getPortNumber();
        String merchantHostname = this.array_of_nodes[merchantNum].getHostName();
        
        try {
            //set up port
            server = new ServerSocket(merchantPort);
            System.out.println("Started a Merchant at: " + merchantHostname + " Port: " + merchantPort);
            int round = 1;
            while (true) {
                System.out.println("Merchant: Waiting for connection");

                //accept connection
                socket = server.accept();
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw = new PrintWriter(socket.getOutputStream(), true);
                System.out.println("Merchant: Connection recieved");
                
                //spilt input to get cipherText
                String in = br.readLine();
                System.out.println(in);
                String[] items = in.split(",", 2);
                System.out.println("Merchant: Message Recieved");
                String person = items[0];

                String[] data;
                //decrypt message
                if (round == 1) {
                    data = decryptRSA(items[1]).split(",");
                    System.out.println("Merchant: Message decrypted with broker public key, checking type of message");
                    round++;
                } else {
                    data = decryptDES(items[1], brokerSym).split(",");
                    System.out.println("Merchant: Message decrypted with broker secret key, checking type of message");
                }

                if (data[0].equals("auth")) {
                    System.out.println("Merchant: It is an auth message");
                    authWithBroker(pw, br);
                    socket.close();
                } else {
                    String[] data2;
                    if (round == 2) {
                        data2 = decryptRSA(data[1]).split(",");
                        round++;
                    } else {
                        data2 = decryptDES(data[1], clientSym).split(",");
                    }

                    if (data2[0].equals("content")) {
                        System.out.println("Merchant: It is a request for content message");

                        //save sym key
                        byte[] decodedKey = Base64.getDecoder().decode(data2[1]);
                        clientSym = new SecretKeySpec(decodedKey, 0, decodedKey.length, "DESede");

                        //send back content
                        socket = new Socket("dc20.utdallas.edu", 1234);
                        pw = new PrintWriter(socket.getOutputStream(), true);
                        String plainText = name + ",";
                        String message = encryptDES(products, clientSym);
                        String middle = encryptDES(data[0] + "," + message, brokerSym);
                        pw.println(plainText + middle);
                        System.out.println("Merchant: Content encrypted with clientSym Key: " + message);
                        System.out.println("Merchant: Sent content list");
                        socket.close();
                    } else if (data2[0].equals("pay")) {
                        System.out.println("Merchant: It is a request for pay and get product");
                        String sel = data2[1];
                        int selectProduct = Integer.parseInt(sel);

                        String prod = products.split(",")[selectProduct];
                        prod = prod.split(" ")[1].trim().toLowerCase();

                        System.out.println("Merchant: Client wants " + prod);

                        String PATH = System.getProperty("user.dir");
                        PATH = PATH + "/CS6349/Project/merchant/src/items/" + prod + ".txt";
                        Path path = Paths.get(PATH);
                        byte[] bytes = Files.readAllBytes(path);

                        String tmp = encryptDES(Base64.getEncoder().encodeToString(bytes) + "," + prod, clientSym);
                        String enc = encryptDES(data[0] + "," + tmp, brokerSym);
                        socket = new Socket("dc20.utdallas.edu", 1234);
                        pw = new PrintWriter(socket.getOutputStream(), true);
                        pw.println(name + "," + enc);
                        System.out.println("Merchant: Product has been shipped to client");
                        round = 2;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }       
    }

    public void authWithBroker(PrintWriter pw, BufferedReader br) {
        try {
            System.out.println("Merchant: generating a nonce");
            //send back nonce
            int random = new Random().nextInt();
            String nonce = "" + random;
            System.out.println("Merchant: Nonce is: " + nonce);
            pw.println(encryptRSA(nonce));
            System.out.println("Merchant: Sending nonce");

            //wait for new reply, check if its nonce + auth
            System.out.println("Merchant: Got reply, checking");
            String[] data = decryptRSA(br.readLine()).split(",");

            if (data[0].equals(nonce + "auth")) {
                System.out.println("Merchant: Nonce correct");
                //Generate symtric keys
                brokerSym = KeyGenerator.getInstance("DESede").generateKey();

                System.out.println("Merchant: shared key is: " + Base64.getEncoder().encodeToString(brokerSym.getEncoded()));

                //send key
                String plainText = data[1] + "auth," + Base64.getEncoder().encodeToString(brokerSym.getEncoded());
                pw.println(encryptRSA(plainText));
                System.out.println("Merchant: Sending shared key");
            } else {
                System.out.println("Nonce Incorrect");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String encryptRSA(String plainText) {
        String cipherText = "";
        try {
            Cipher encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, brokerP);
            byte[] bytes = encryptCipher.doFinal(plainText.getBytes(UTF_8));
            cipherText = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipherText;
    }

    public String decryptRSA(String cipherText) {
        String plainText = "";
        try {
            Cipher decriptCipher = Cipher.getInstance("RSA");
            decriptCipher.init(Cipher.DECRYPT_MODE, merchantPr);
            byte[] bytes = Base64.getDecoder().decode(cipherText);
            plainText = new String(decriptCipher.doFinal(bytes), UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return plainText;
    }

    public String encryptDES(String plainText, SecretKey key) {
        String cipherText = "";
        try {
            Cipher encryptCipher = Cipher.getInstance("DESede");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] bytes = encryptCipher.doFinal(plainText.getBytes(UTF_8));
            cipherText = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipherText;
    }

    public String decryptDES(String cipherText, SecretKey key) {
        String plainText = "";
        try {
            Cipher decriptCipher = Cipher.getInstance("DESede");
            decriptCipher.init(Cipher.DECRYPT_MODE, key);
            byte[] bytes = Base64.getDecoder().decode(cipherText);
            plainText = new String(decriptCipher.doFinal(bytes), UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return plainText;
    }

    public void loadKeys(String key) {
        try {
            String PATH = System.getProperty("user.dir");
            PATH = PATH + "/CS6349/Project/merchant/src";
            PKCS8EncodedKeySpec pkcs8 = null;
            X509EncodedKeySpec x509 = null;
            KeyFactory kf = KeyFactory.getInstance("RSA");

            // Read all bytes from the merchant private key file
            Path path = Paths.get(PATH + key + ".key");
            byte[] bytes = Files.readAllBytes(path);
            // Generate private key.
            pkcs8 = new PKCS8EncodedKeySpec(bytes);
            merchantPr = kf.generatePrivate(pkcs8);

            // Read all the merchant public key bytes
            path = Paths.get(PATH + key + ".pub");
            bytes = Files.readAllBytes(path);
            // Generate public key.
            x509 = new X509EncodedKeySpec(bytes);
            merchantP = kf.generatePublic(x509);

            // Read all the broker public key bytes
            path = Paths.get(PATH + "/brokerKey.pub");
            bytes = Files.readAllBytes(path);
            // Generate public key.
            x509 = new X509EncodedKeySpec(bytes);
            brokerP = kf.generatePublic(x509);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}