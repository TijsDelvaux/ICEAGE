package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Server   {

    private ServerSocket serverSocket;
    private int port = 4444;

    private List<String> clientNames;
    private Map<String,Integer> clientCounts;
    private Set<String> excludedList;
    int totalNbPickedUp;
    int total;


    public Server(){
        totalNbPickedUp = 0;
        total = 56;
        clientNames = new ArrayList<String>();
        clientCounts = new HashMap<String, Integer>();
        for(String name: clientNames) {
            clientCounts.put(name, 0);
        }
        excludedList = new HashSet<String>();

        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }

    public static void main(String[] args) {
        Server server = new Server();
    }


    public void excludeFromList(String image){
        excludedList.add(image);
    }

    private class SocketServerThread extends Thread {

        private int SocketServerPORT = port;

        @Override
        public void run() {
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                System.out.println("I'm waiting here: " + serverSocket.getLocalPort());
                System.out.println("IP: " + getIpAddress());

                while (true) {
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    String messageFromClient = "";

                    //If no message sent from client, this code will block the program
                    messageFromClient = dataInputStream.readUTF();

//                    System.out.println(messageFromClient);

                    String[] splitMessage = messageFromClient.split(":");
                    String clientName = splitMessage[0];
                    String messageCode = splitMessage[1];
                    String msg = splitMessage[2];

                    if(!clientNames.contains(clientName)){
                        clientNames.add(clientName);
                        clientCounts.put(clientName,0);

                        System.out.println("[SERVER] A new client has registered: " + clientName);
                        dataOutputStream.writeUTF("1:" + "Welcome to the IceAge Nut Discovery game, " + clientName + "!");
                    }

                    String printMessage = "";
                    String reply = "";


                    switch (Integer.parseInt(messageCode)){
                        //Client picked up an acorn, add the picture-name to the excluded list
                        //TODO add zones!!
                        case 0: // pickup achorn
                            if(clientPickUpAchorn(clientName, msg)) {
                                printMessage = "[SERVER] " + clientName + "picked up an achorn\n" +
                                        "~~~~~~ " + clientName + ": " + clientCounts.get(clientNames) + ";" +
                                        " total count: " + totalNbPickedUp + "; total left: " + (total - totalNbPickedUp) + ")"
                                        + getLeaderBoardString();
                                reply = "1:" + "You have picked up an achorn! \n" +
                                        totalNbPickedUp + " of the " + total + " achorns are found";
                            }
                            else {
                                printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up achorn," +
                                        "but this failed (client was not registered or achorn was not there).";
                                reply = "1:" + "Oops, something went wrong, you were not able to pick up the achorn.";
                            }
                            break;

                        case 1: // other messages
                            //TODO nothing happens with these messages!
                            printMessage = "[SERVER] " + clientName + ": " + msg;
                            reply = "1:" + "Message received";
                            break;

                        case 2: // enter a new zone
                            //TODO entering new zone, update excluded list on client
                            printMessage = "[SERVER] " + clientName + " entered a new zone (" + msg + ")";
                            reply = "2:" + msg;
                            break;

                        case 3: //Check if the asked picture is in the excluded list
                            if(clientRequestAchorn(msg)){
                                reply = "3:free:" + msg;
                                printMessage = "[SERVER] " + clientName + " requested achorn ("+ msg +")" +
                                        " and it is free!";
                            }
                            else{
                                reply = "3:" + msg;
                                printMessage = "[SERVER] " + clientName + " requested achorn ("+ msg +")" +
                                        ", but it has been taken";
                            }

                            break;

                        default:
                            printMessage = "[SERVER] ERROR - Something went wrong, invalid messagecode";
                            reply = "Oops, something went wrong";
                            break;

                    }

                    System.out.println(printMessage);
                    dataOutputStream.writeUTF(reply);
                }
            } catch (IOException e) {
                e.printStackTrace();
                final String errMsg = e.toString();
                System.out.println(errMsg);

            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public String getIpAddress() {
            String ip = "";
            try
            {
                Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                        .getNetworkInterfaces();
                while (enumNetworkInterfaces.hasMoreElements())
                {
                    NetworkInterface networkInterface = enumNetworkInterfaces
                            .nextElement();
                    Enumeration<InetAddress> enumInetAddress = networkInterface
                            .getInetAddresses();
                    while (enumInetAddress.hasMoreElements())
                    {
                        InetAddress inetAddress = enumInetAddress.nextElement();

                        if (inetAddress.isSiteLocalAddress())
                        {
                            ip += "SiteLocalAddress: "
                                    + inetAddress.getHostAddress() + "\n";
                        }

                    }

                }

            } catch (SocketException e)
            {
                e.printStackTrace();
                ip += "Something Wrong! " + e.toString() + "\n";
            }
            return ip;
        }

        /*
         * @return: true if the client was able to pickup the achorn
         */
        private boolean clientPickUpAchorn(String clientName, String imageName) {
            if(clientCounts.containsKey(clientName) && !excludedList.contains(imageName)) {
                excludedList.add(imageName);
                clientCounts.put(clientName, clientCounts.get(clientName) + 1);
                totalNbPickedUp++;
                return true;
            }
            else {
                return false;
            }
        }

        /*
         * @return: true if the achorn is there, false if it is not
         */
        private boolean clientRequestAchorn(String imageName) {
            return !excludedList.contains(imageName);
        }

        private String getLeaderBoardString() {
            String indent = "    ";
            String leaderboard = indent + "LEADERBOARD\n";
            for(String name: clientNames) {
                leaderboard += indent + name + "\t\t" + clientCounts.get(name) + "\n";
            }
            leaderboard += "\n";
            return leaderboard;
        }
    }
}