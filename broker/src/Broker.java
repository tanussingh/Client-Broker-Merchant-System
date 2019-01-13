
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import java.nio.file.*;
import javax.crypto.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;

public class Broker extends Thread {

    //Keys, public private will load from other files, secretket will generate as we go
    KeyGenerator keygen;
    PrivateKey brokerPr;
    PublicKey brokerP;
    Map<String, ConnectionState> connections;

    Broker() {
        Thread t = new Thread(this);
        t.start();
    }

    public void run() {

        connections = new HashMap<>();
        loadKeys();
        connectToMerchant("merchant1");
        connectToMerchant("merchant2");

        //initilize keys
        try {
            keygen = KeyGenerator.getInstance("DESede");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //initialize socket and input/output stream
        ServerSocket server = null;
        Socket socket = null;
        PrintWriter pw = null;
        BufferedReader br = null;

        //initilize merchant host and port
        int port = 1234;
        String host = "dc20.utdallas.edu";

        try {
            server = new ServerSocket(port);
            System.out.println("Started a Broker at: " + host + " Port: " + 1234);
            System.out.println("Waiting for message");

            while (true) {
                System.out.println("Broker: Waiting for connection");

                //accept message
                socket = server.accept();
                System.out.println("Broker: Connection recieved");

                //read in message
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println("Broker: Message Recieved");

                //spilt input to get cipherText
                String[] items = br.readLine().split(",");
                String person = items[0];
                System.out.println(person);

                ConnectionState connection = connections.get(person);

                if (isMerchant(person)) {
                    //Ideally, we would inspect this after a payment to make sure the payment was successful, and reverse the transaction on failure
                    //Instead, we're just going to passthrough on all merchant-client connections:
                    //read sessionId
                    //determine appropriate user based on sessionId, pass through
                    socket.close();
                    String[] data = symmetricDecrypt(items[1], connection.symmetricKey).split(",");
                    String sessionId = data[0];
                    sendMessage(connections.get(sessionId), data[1]);
                } else {
                    //we need to handle a client request
                    if (connection.symmetricKey == null) {
                        System.out.println("Authorizing");
                        //begin auth process
                        authorize(socket, connection, items[1]);
                    } else {
                        socket.close();
                        handleClientRequest(connection, items[1]);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void handleClientRequest(ConnectionState state, String request){
        request = symmetricDecrypt(request, state.symmetricKey);
        String[] content = request.split(",");
        System.out.println("Request: " + content[0]);
        switch(content[0]){
            case "req":
                ConnectionState merchant1 = connections.get("merchant1");
                ConnectionState merchant2 = connections.get("merchant2");
                sendMessage(state, "merchant1," + Base64.getEncoder().encodeToString(merchant1.publicKey.getEncoded())
                                + ",merchant2," +  Base64.getEncoder().encodeToString(merchant2.publicKey.getEncoded()));
                //send back merchants+their keys
                break;
            case "choose":
                //store the merchant they chose, then pass along
                state.choice = content[1];
                ConnectionState merchantState = connections.get(content[1]);
                state.sessionId = UUID.randomUUID().toString();
                connections.put(state.sessionId, state);
                
                sendMessage(merchantState, state.sessionId + "," + content[2]);
                
                System.out.println("Sent a message to merchant for content");
                break;
            case "pay":
                //in practice, we would perform business logic to validate payment
                //we just assume the positive case and pass along.
                //also, we would log this payment as verified
                merchantState = connections.get(state.choice);
                
                String[] validation = publicDecrypt(content[1], state.publicKey).split(",");
                
                System.out.println("Client confirmed a payment of " + validation[0] + "with timestamp " + validation[1]);
                
                //could validation validation[1] to make sure time hasn't been used or is within some range or something
                
                sendMessage(merchantState, state.sessionId + ","+ content[2] + "," + validation[0]);
                break;
            default:
                System.out.println("Invalid request");
        }
    }

    public void authorize(Socket socket, ConnectionState state, String request) {
        try {
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            request = publicDecrypt(request);
            
            System.out.println(request);
            if (request.equals("auth")) {
                System.out.println("Broker: It is an auth message, generating a nonce");
                //send back nonce
                String nonce = "" + new Random().nextInt();
                System.out.println("Broker: Sending nonce");
                pw.println(publicEncrypt(nonce, state.publicKey));

                //wait for new reply, check if its nonce + auth
                System.out.println("Broker: Got reply, checking");
                String plainText = publicDecrypt(br.readLine());
                String[] data = plainText.split(",");

                if (data[0].equals(nonce + "auth")) {
                    System.out.println("Broker: Nonce correct");
                    //Generate symtric keys
                    state.symmetricKey = keygen.generateKey();

                    System.out.println("Broker: shared key is: " + Base64.getEncoder().encodeToString(state.symmetricKey.getEncoded()));

                    //send key
                    plainText = data[1] + "auth," + Base64.getEncoder().encodeToString(state.symmetricKey.getEncoded());

                    pw.println(publicEncrypt(plainText, state.publicKey));
                    System.out.println("Broker: Sending shared key");
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean isMerchant(String name) {
        return name.equals("merchant1") || name.equals("merchant2");
    }

    public void sendMessage(ConnectionState state, String message) {
        System.out.println("Sending to " + state.host + ":" + state.port + "  -- " + message);
        try {
            Socket connection = new Socket(state.host, state.port);
            PrintWriter pw = new PrintWriter(connection.getOutputStream(), true);
            pw.println("broker," + symmetricEncrypt(message, state.symmetricKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectToMerchant(String merchant) {
        try {

            ConnectionState state = connections.get(merchant);

            Socket connection = new Socket(state.host, state.port);

            String message = "broker," + publicEncrypt("auth", state.publicKey);

            System.out.println("writing auth request");
            PrintWriter pw = new PrintWriter(connection.getOutputStream(), true);
            pw.println(message);

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            System.out.println("waiting for nonce");

            String nonce = publicDecrypt(br.readLine());

            nonce = nonce + "auth";

            String myNonce = "" + new Random().nextInt();

            message = nonce + "," + myNonce;

            pw.println(publicEncrypt(message, state.publicKey));

            String[] data = publicDecrypt(br.readLine()).split(",");

            nonce = data[0];

            if (nonce.equals(myNonce + "auth")) {
                byte[] decodedKey = Base64.getDecoder().decode(data[1]);
                // rebuild key using SecretKeySpec
                SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "DESede");

                state.symmetricKey = originalKey;
                System.out.println("authentication succeeded, key: " + data[1]);
            } else {
                System.out.println("authentication failed");
            }
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String publicDecrypt(String cipherText) {
        String ret = null;
        try {
            Cipher pkc = Cipher.getInstance("RSA");
            pkc.init(Cipher.DECRYPT_MODE, brokerPr);
            byte[] base64 = Base64.getDecoder().decode(cipherText);
            ret = new String(pkc.doFinal(base64), UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
    
    public String publicDecrypt(String cipherText, PublicKey key) {
        String ret = null;
        try {
            Cipher pkc = Cipher.getInstance("RSA");
            pkc.init(Cipher.DECRYPT_MODE, key);
            byte[] base64 = Base64.getDecoder().decode(cipherText);
            ret = new String(pkc.doFinal(base64), UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String publicEncrypt(String plainText, PublicKey key) {
        String ret = null;
        try {
            Cipher pkc = Cipher.getInstance("RSA");
            pkc.init(Cipher.ENCRYPT_MODE, key);
            byte[] bytes = pkc.doFinal(plainText.getBytes());
            ret = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String symmetricDecrypt(String cipherText, SecretKey key) {
        String ret = null;
        try {
            Cipher pkc = Cipher.getInstance("DESede");
            pkc.init(Cipher.DECRYPT_MODE, key);
            byte[] base64 = Base64.getDecoder().decode(cipherText);
            ret = new String(pkc.doFinal(base64), UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String symmetricEncrypt(String plainText, SecretKey key) {
        String ret = null;
        try {
            Cipher pkc = Cipher.getInstance("DESede");
            pkc.init(Cipher.ENCRYPT_MODE, key);
            byte[] bytes = pkc.doFinal(plainText.getBytes());
            ret = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void loadKeys() {
        try {
            PKCS8EncodedKeySpec pkcs8 = null;
            X509EncodedKeySpec x509 = null;
            KeyFactory kf = KeyFactory.getInstance("RSA");

            // Read all bytes from the merchant private key file
            byte[] bytes =             
            Base64.getDecoder().decode("MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCLaYdQOz+oBoXs+M3e+4WntHGb06nNCY2wLFBNfL/6cbyNgNnHIwu/miC6JpzAZ+xhAiCsTFQtQamfMjmtTIzygz+7A+lkTyeHGhZLoR2CbGwXC7YtYlxjvtWaGnbkpK449zWyV3n/KHqObi+yUUpjKRRnnLhHsbeDMphhi8US8SAqiVA1kr/mp3GmvCpjmzoBVQrx365JpALaFQgrrCoqYZ2oQKySbN2Yb4It9SI9zDrLk5xi+UV5M0ihZHxwFZML6yNN9icd+TbASGe80PU4oeCPQg5xu6ww2jF/HTUyH0Ra9djYR4aTvs3LuCJYvyThy4RGNduQRDOV4NHXsfTPAgMBAAECggEASE8L6AlTEwuPG0JRRX6f7EQjSPeX8skpvF6/p/E0genMKnjSe/8pcM+4edTdKM6+Q/Kej79nSbHtEK00TTaPRJnezFlTDLwhfGmDduayL5uc1Lc2XoPN985ba/qeACmKU/Gk4EOO+1E7f/SWuJ6BPr1n3/Xhfw4maN4tOysLpK63LWXr2TyU+p2IkSev+I+ddaq1iCgnN8Oqc5G6VCAadzLhwEGJa38WCY/rAO30F3H1+XcnBFJgko4PSrUVBediNbyrVfMBoRO9QPeCq+JnI9cEObckQdbjD3N6iYljSABdS6aIIjaOdSVZxiPF7+tO5b77b6VWMGQvO22gTa5OAQKBgQDSV0yrP585l+alBX5kn9/4c2X+6QtSfkkeVe3X9rLo5V1StyHopPy1KBWukJ1oGTGX68+XajvU8/DbF9/i6MQ/f7j2yAikfLO4YEfuLaW9KHdQrfm7tjq6vEMM52e4QbvgMGlEM0deq1RN7ex1KouUBkPBR27pvaKEHBtRCd4fIQKBgQCprLL3BuDM7RHfBAxrcuE6Z7v92rIkPrYIjRlJWN0wdxqjXU2v4wLal3fbUfagneahV5AzhLBjh56YfUEfUfa11hH+HIDxo5SsjQhXEixXSR2rDAjGdtsZiKobRsOrrqDtJK0WPEwBQpSup2in6TTXCkzEwLbf00bFlxOO8A9F7wKBgHCRGc3X6Z2H5n5QF1lAmjs7fs1R6KZIQVdDw1q9gvfSsAPxT+tSSI+mmRvn9uVb/keAgoNU7hpERpTqDP5Bda1J7DHd0Yo91myI0lXsBfPacSgzQyArIPkIgZWpTb+1JhePsPY3vy0x4ZcnClGV6Ebap24LjZb3zr6G0DOpZT8hAoGAeBXJJ7oPehnDDzK/U4Cf1QU78MNKVwqnLNAn+FhTW4zAJqTPac9h4rFW27tbsHtwkfn2DfA4IokGfugPIgqRcEpMu6sSu3JJtAGwyGcNSM1vmPJQd54BHUYzFlD0BVr3fD773YPZSv8DWcUT9drAUf1xLcMy6qCOMyPkbMKMHLMCgYEAk0lBwYs7zBw0ZS87ZFzkc74qI82edW/ibLFIChzsB9c2uS+JbLjuCpHmyxE5nGJ/pdf5PLC4ruHZWj/Pd3/faBy9qsuQXLgXXtycqLhjBlmSR3ugISCUJATXsPZIs2bEEkNnGUNx+V1STebt9+F5STHz2bcCQVWzajV+O3dMbc4=");
            // Generate private key.
            pkcs8 = new PKCS8EncodedKeySpec(bytes);
            brokerPr = kf.generatePrivate(pkcs8);


            // Read all the merchant public key bytes
            bytes = Base64.getDecoder().decode("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv3sE6eUjdOQqgfnyWSRvZbHKojZGpPnTA7zQ0oVQMyUFCa7q1JtjUzRlEZgZEt9SvCrno0yiCeCeCeOqjeQawpmh3qy3CSysQaoIKos6LrXqT14H5EshqHL8wFJTG/IjQ3YhefdYvvwE9Gq3HIescDPRq5V7mhZo9qfu8GrzM8cf61EqbU0iAOo0pNXgeqH6ioGjL/mHkihBg1KgN7F/azzxhTmoZ6y2Txi+rejyvDtlOVewwuAWzldlODjkxVq9SsL5PxN6bjuM/OGWhl4EcX5v8QtiX2Nf38a4cyPw3Iyvx6LsoYOIiWLhQU5+avCEkqMhRI98qZGcsyIGpA++/QIDAQAB");
            // Generate public key.
            x509 = new X509EncodedKeySpec(bytes);
            ConnectionState merchant1 = new ConnectionState();
            merchant1.host = "dc15.utdallas.edu";
            merchant1.port = 1234;
            merchant1.publicKey = kf.generatePublic(x509);
            connections.put("merchant1", merchant1);

            // Read all the broker public key bytes
            bytes = Base64.getDecoder().decode("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtb5Xh42rbXLI+L9n7HMTDkGlh6dcWTCxfMHNQpZyMwXa5wJ/lt3J37Xs9inKdu/8Wcc0FzU5wl/GtdNSxh/MrSF9QeNdYCLeBnfWgV53A8XShF/jXKDlNJqeUGFTTNXm5OTF3BJJGdCt5AKvyJe7E5rHqt6Lgsy6G4dzn/O/NQLx734CAp9oLJC/2pwjmyzSs08oBlp7BXHDtxyHguRdSKIYW1u/YH/YPe5Tfl1AVTNI9rQ09MRPpKD6OWAld144nQQHUO3a9ek8Sqw/dutIkqajeLR0cCmVZRafzLNU5jhAqEWcdoCmBqhg1ZVydmzoqoKtBSTE0CAQ+CiRUd7oiwIDAQAB");
            // Generate public key.
            x509 = new X509EncodedKeySpec(bytes);
            ConnectionState merchant2 = new ConnectionState();
            merchant2.host = "dc16.utdallas.edu";
            merchant2.port = 1234;
            merchant2.publicKey = kf.generatePublic(x509);
            connections.put("merchant2", merchant2);

            // Read all the broker public key bytes
            bytes = Base64.getDecoder().decode("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiXF8PjhfecRmt7u3kMuB3i9Tnqa3ZdR+d9GE2KmCMRiprPRz7cuYptinJqlBl3iAWGmskq5wpLNUwsNNhhg8GI9R9NjnulTnMaaljSOhfiLWCzH48t8ruFOkk0eo5L8oJa1YVwC4LfNnbVawRqoSWC7pBGEyvoRNKbVsiWdKK7NIrP5DVVIXYzQxxJ+mGc5kx6H725Eaqrrj6KH16rgRGE9U6j3uKZ40YsN3IFFNfUIoEySXoC3vBAAnXL9mAvvKXEa5u4ovClZGC6XyPaGOgMP8G+cpmew8K60OrOOc7RHkrw3U1RgASRL+vjkQuueohmOpGCn2Mv8Dnc8/TE+FtwIDAQAB");
            // Generate public key.
            x509 = new X509EncodedKeySpec(bytes);
            ConnectionState client = new ConnectionState();
            client.host = "dc10.utdallas.edu";
            client.port = 1234;
            client.publicKey = kf.generatePublic(x509);
            connections.put("client", client);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
