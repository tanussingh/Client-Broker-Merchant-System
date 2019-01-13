/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;
import java.lang.*;
import javax.crypto.*;
import java.security.*;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NSBroker {
    public static void main(String[] args) {
        
	//start all 5 merchants
        Broker bro = null;
        try{
            bro = new Broker();
            System.in.read();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.exit(0);
        
    }
}