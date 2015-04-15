package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress; 
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a simple server application. This server receive a string message
 * from the Android mobile phone and show it on the console.
 * Author by Lak J Comspace
 */
public class Server {
 
  private static ServerSocket serverSocket;
  private static Socket clientSocket;
  private static InputStreamReader inputStreamReader;
  private static BufferedReader bufferedReader;
  private static String message;

  //Deze map bevat voor elke zone een lijst van NIET beschikbare targets (dingen waarop een eikel wordt getoont)
  //Als een persoon een zone binnenstapt moet de juiste lijst (en misschien de lijsten van naburige zones)
  //naar die persoon gestuurd worden.
  //Wanneer een target de app een target detecteert, dan zal deze kijken naar deze lijst.
  //Als de naam van de target in de lijst zit, dan verschijnt er geen eikel.
  //{<"midden_foyer",["foto1",foto9"]>,<"",["foto4",foto2"]>,...}
  private HashMap<String, ArrayList<String>> excludedTargets = new HashMap<String, ArrayList<String>>();
 
  public static void main(String[] args) {
    try {
      serverSocket = new ServerSocket(4444); // Server socket
 
    } catch (IOException e) {
      System.out.println("Could not listen on port: 4444");
    }
 
    String ip = "failed to get ip-address";
	try {
		ip = InetAddress.getLocalHost().getHostAddress();
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    System.out.println("Server started. Listening to the port 4444 on " + ip);
 
    while (true) {
      try {
 
        clientSocket = serverSocket.accept(); // accept the client connection
        System.out.println("Connection with: " + clientSocket.getInetAddress().getHostName());
        inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
        bufferedReader = new BufferedReader(inputStreamReader); // get the client message
        message = bufferedReader.readLine();
 
        System.out.println(message);
        inputStreamReader.close();
        clientSocket.close();
 
      } catch (IOException ex) {
        System.out.println("Problem in message reading");
      }
    }
 
  }
 
}