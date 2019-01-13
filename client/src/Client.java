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
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Client extends Thread{
    //set up variables
    Nodes client = new Nodes(0, "dc10.utdallas.edu", 1234);
    Nodes broker = new Nodes(1, "dc20.utdallas.edu", 1234);

    //Keys, public private will load from other files, secretket will generate as we go
    KeyGenerator keygen;
    PrivateKey clientPr;
    PublicKey clientP;
    PublicKey brokerP;
    PublicKey merchant1;
    PublicKey merchant2;
    SecretKey brokerSym;
    SecretKey merchantSym;

    Client () {
        Thread t = new Thread(this);
        t.start();
    }

    public void run() {
        //initilize keys
        loadKeys();

        //initialize socket and input/output stream
        ServerSocket server = null;
        Socket socket = null;
        PrintWriter pw = null;
        BufferedReader br = null;

        //initilize merchant host and port
        int clientPort = this.client.getPortNumber();
        String clientHostname = this.client.getHostName();

        try {
            //set up port
            server = new ServerSocket(clientPort);
            System.out.println("Started a Client at: " + clientHostname + " Port: " + clientPort);
            
            //accept connection
            socket = new Socket(broker.getHostName(), broker.getPortNumber());
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Generating connection with Broker:-");

            //build message to auth with broker
            String header = "client,";
            String plainText = "auth";

            //send message
            pw.println(header+encryptRSA(plainText));
            System.out.println("Client: Sending request message");

            //get message back
            String nonce = decryptRSA(br.readLine());
            System.out.println("Client: Got nonce back");

            //add auth to nonce, send my nonce
            nonce = nonce + "auth,";
            int random = new Random().nextInt();
            String myNonce = "" + random;
            pw.println(encryptRSA(nonce+myNonce));
            System.out.println("Client: Sending back nonce, and my own nonce");

            //get message back
            String[] data = decryptRSA(br.readLine()).split(",");
            System.out.println("Client: Auth successful");

            //check if nonce is correct
            if (data[0].equals(myNonce + "auth")) {
                System.out.println("Shared key between client and broker is: " + data[1]);
                byte[] decodedKey = Base64.getDecoder().decode(data[1]);
                brokerSym = new SecretKeySpec(decodedKey, 0, decodedKey.length, "DESede");
            }

            boolean cont = true;

            //connection is disconnected
            socket = new Socket(broker.getHostName(), broker.getPortNumber());
            pw = new PrintWriter(socket.getOutputStream(), true);
            plainText = "req";
            pw.println(header + encryptDES(plainText, brokerSym));
            System.out.println("Client: sent request to broker for merchant list");

            //connection is disconnected
            socket = server.accept();
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String[] merchantList = decryptDES(br.readLine().split(",")[1], brokerSym).split(",");

            while(cont) {
                System.out.println("Hello, this is the list of merchants: ");
                System.out.println("1. " + merchantList[0]);
                System.out.println("2. " + merchantList[2]);
                System.out.print("Please select a merchant: ");
                char selection = (char) System.in.read();
                System.in.read();

                X509EncodedKeySpec x509 = null;
                KeyFactory kf = KeyFactory.getInstance("RSA");
                x509 = new X509EncodedKeySpec(Base64.getDecoder().decode(merchantList[1]));
                merchant1 = kf.generatePublic(x509);

                x509 = new X509EncodedKeySpec(Base64.getDecoder().decode(merchantList[3]));
                merchant2 = kf.generatePublic(x509);

                merchantSym = KeyGenerator.getInstance("DESede").generateKey();
                socket = new Socket(broker.getHostName(), broker.getPortNumber());
                pw = new PrintWriter(socket.getOutputStream(), true);

                PublicKey selectedPublicKey = selection == '1' ? merchant1 : merchant2;
                
                System.out.println("Client: You have selected merchant " + selection);
                String message = encryptRSA("content," + Base64.getEncoder().encodeToString(merchantSym.getEncoded()), selectedPublicKey);
                pw.println(header+encryptDES("choose,merchant" + selection + "," + message, brokerSym));

                //connection is disconnected
                socket = server.accept();
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                data = decryptDES(decryptDES(br.readLine().split(",")[1], brokerSym), merchantSym).split(",");
                System.out.println("Client: Getting catalog from merchant " + selection);

                //connection is disconnected
                System.out.println(data[0] + ", Price: " + data[6]);
                System.out.println(data[1] + ", Price: " + data[7]);
                System.out.println(data[2] + ", Price: " + data[8]);
                System.out.println(data[3] + ", Price: " + data[9]);
                System.out.println(data[4] + ", Price: " + data[10]);
                System.out.println(data[5] + ", Price: " + data[11]);
                System.out.print("Please select a product: ");
                int productSelect = System.in.read() - '0';
                System.in.read();

                socket = new Socket(broker.getHostName(), broker.getPortNumber());
                pw = new PrintWriter(socket.getOutputStream(), true);

                System.out.println("Client: You have selected product " + productSelect + ", " + data[productSelect - 1]);
                System.out.println("Client: You will be paying " + data[productSelect + 5]);
                long currentTime = System.currentTimeMillis();
                String time = "" + currentTime;

                String msg = encryptDES("pay," + (productSelect - 1), merchantSym);
                String encRSA = encryptRSA(data[productSelect + 5] + "," + time, clientPr);
                pw.println(header+encryptDES("pay," + encRSA + "," + msg, brokerSym));

                //connection is disconnected
                socket = server.accept();
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println("Client: You have recieved a product, decoding...");

                String[] purchased = decryptDES(decryptDES(br.readLine().split(",")[1], brokerSym), merchantSym).split(",");

                String file = purchased[1];
                System.out.println("Client: You got the product: " + file);
                FileOutputStream out = new FileOutputStream(file + ".txt");
                out.write(Base64.getDecoder().decode(purchased[0]));
                out.close();

                System.out.println("Do you want to purchase another item? (y, n): ");
                cont = 'y' == System.in.read();
                System.in.read();
            }

        }catch(Exception e){
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

    public String encryptRSA(String plainText, PublicKey key) {
        String cipherText = "";
        try {
            Cipher encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] bytes = encryptCipher.doFinal(plainText.getBytes(UTF_8));
            cipherText = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipherText;
    }

    public String encryptRSA(String plainText, PrivateKey key) {
        String cipherText = "";
        try {
            Cipher encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
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
            decriptCipher.init(Cipher.DECRYPT_MODE, clientPr);
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

    public void loadKeys() {
        try {
            String PATH = System.getProperty("user.dir");
            //PATH = PATH + "/CS6349/Project/client/src";
            PKCS8EncodedKeySpec pkcs8 = null;
            X509EncodedKeySpec x509 = null;
            KeyFactory kf = KeyFactory.getInstance("RSA");

            // Read all bytes from the client private key file
            Path path = Paths.get(PATH + "/clientKey.key");
            byte[] bytes = Files.readAllBytes(path);
            // Generate private key.
            pkcs8 = new PKCS8EncodedKeySpec(bytes);
            clientPr = kf.generatePrivate(pkcs8);

            // Read all the client public key bytes
            path = Paths.get(PATH + "/clientKey.pub");
            bytes = Files.readAllBytes(path);
            // Generate public key.
            x509 = new X509EncodedKeySpec(bytes);
            clientP = kf.generatePublic(x509);

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

