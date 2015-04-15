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

public class Server   {

    private ServerSocket serverSocket;
    private int port = 4444;

    private ArrayList<String> excludedList;


    public Server(){
        excludedList = new ArrayList<String>();
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

                    System.out.println(messageFromClient);
                    String[] splitMessage = messageFromClient.split(":");
                    String clientName = splitMessage[0];
                    String messageCode = splitMessage[1];
                    String msg = splitMessage[2];

                    String printMessage = "";

                    switch (Integer.parseInt(messageCode)){
                        //Client picked up an acorn, add the picture-name to the excluded list
                        //TODO add zones!!
                        case 0:
                            excludeFromList(msg);
                            printMessage = "I picked up: " + msg;
                            break;
                        //Just a message
                        case 1:
                            printMessage = msg;
                            break;
                        default:
                            printMessage = "Something went wrong, invalid messagecode";
                            break;
                    }


                    String message = clientName + ": " + printMessage;
                    System.out.println(message);

                    String msgReply = "0:rubbish";
                    System.out.println("Sending message to " + clientName + ": " + msgReply);
                    dataOutputStream.writeUTF(msgReply);

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