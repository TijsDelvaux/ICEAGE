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
import java.util.HashSet;

public class Server   {

    private ServerSocket serverSocket;
    private int port = 4444;

    private ArrayList<String> clientNames;
    private HashSet<String> excludedList;
    int count;
    int total;


    public Server(){
        count = 0;
        total = 56;
        clientNames = new ArrayList<String>();
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
                    }

                    String printMessage = "";
                    String reply = "";


                    switch (Integer.parseInt(messageCode)){
                        //Client picked up an acorn, add the picture-name to the excluded list
                        //TODO add zones!!
                        case 0:
                            excludedList.add(msg);
                            count++;
                            printMessage = "I picked up: " + msg;
                            String message = clientName + ": " + printMessage;
                            System.out.println(message);
                            reply = "1:" + count + "/" + total + " are found";
                            break;
                        //Just a message
                        case 1:
                            printMessage = msg;
                            reply = "0:0";
                            message = clientName + ": " + printMessage;
                            System.out.println(message);
                            break;
                        case 2:
                            //TODO entering new zone, update excluded list on client
                            printMessage = "Shouldn't be able to get here yet...";
                            reply = "0:0";
                            break;
                        //Check if the asked picture is in the excluded list
                        case 3:
                            boolean isTaken = excludedList.contains(msg);
                            if(isTaken){
                                reply = "3:" + msg;
                                printMessage = "This picture ("+ msg +") has already been taken";
                            }else{
                                reply = "3:free:" + msg;
                                printMessage = "This picture ("+ msg +") is free!";
                            }

                            break;
                        default:
                            printMessage = "Something went wrong, invalid messagecode";
                            reply = "0:0";
                            break;

                    }


//                    String message = clientName + ": " + printMessage;
//                    System.out.println(message);

//                    System.out.println("Sending message to " + clientName + ": " + reply);
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

    }
}