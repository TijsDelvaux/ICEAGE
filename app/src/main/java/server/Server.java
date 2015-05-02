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

    /*
     * TEAMS:
     *  - a client must be in exactly one team
     *  - a team must contain at least one client
     */
    private Map<String,String> clientTeams; // specify for each client in which team they are
    private Map<String,List<String>> teamClients; // specify a list of clients for each team

    private Map<String,Integer> clientCounts; // specify for each client how many acorns they have picked up
    private Map<String,Integer> teamCounts; // specify for each team how many acorns they have picked up

    private Set<String> excludedList;

    private int totalNbPickedUp;
    private final int totalNbAcorns = 56;

    public static void main(String[] args) {
        new Server();
    }

    public Server(){
        totalNbPickedUp = 0;
        clientTeams = new HashMap<String, String>();
        teamClients = new HashMap<String, List<String>>();
        clientCounts = new HashMap<String, Integer>();
        teamCounts = new HashMap<String, Integer>();
        excludedList = new HashSet<String>();

        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }

    public void excludeFromList(String image){
        excludedList.add(image);
    }

    /*
    * @return: true if the client was able to pickup the acorn
    */
    private boolean clientPickUpAcorn(String clientName, String teamName, String imageName) {
        if(clientCounts.containsKey(clientName) && !excludedList.contains(imageName)) {
            // pickup
            excludeFromList(imageName);
            clientCounts.put(clientName, clientCounts.get(clientName) + 1);
            updateTeamCount(teamName);
            totalNbPickedUp++;

            // notify all team players
            if(clientTeams.get(clientName).equals(teamName)) {
                // find all teamplayers
                for(String client: teamClients.get(teamName)) {
                    // make sure you do not notify yourself
                    if(!client.equals(clientName)) {
                        notifyOfPickup(clientName, client);
                    }
                }
            }
            else {
                // this client says that he is in an other team than the server thinks he is; this should not happen
                return false;
            }
            return true;
        }
        else {
            // this client is not registered; this should not happen
            return false;
        }
    }

    public void updateTeamCount(String teamName){
        int teamTotal = 0;
        for(String player: teamClients.get(teamName)){
            teamTotal += clientCounts.get(player);
        }
        teamCounts.put(teamName, teamTotal);
    }

    /*
     * @return: true if the acorn is there, false if it is not
     */
    private boolean clientRequestAcorn(String imageName) {
        return !excludedList.contains(imageName);
    }

    /*
     * @return: a pretty String of the current leaderboard
     */
    private String getLeaderBoardString() {
        String indent = "    ";
        String leaderboard = "\n" + indent + "LEADERBOARD USERS\n";
        for(String name: clientCounts.keySet()) {
            leaderboard += indent + name + "\t\t" + clientCounts.get(name) + "\n";
        }
        leaderboard += "\n";
        leaderboard += "\n" + indent + "LEADERBOARD TEAMS\n";
        for(String name: teamCounts.keySet()) {
            leaderboard += indent + name + "\t\t\t" + teamCounts.get(name) + "\n";
        }
        leaderboard += "\n";
        return leaderboard;
    }

    private void notifyOfPickup(String myName, String hisName) {
        //TODO MsgClient.TEAMMATE_PICKUP
    }

    /*
     * @return: true if the registration succeeded
     */
    private boolean registerNewClient(String clientName, String teamName){
        //TODO if you start a new game with the same name, your current acorns will be lost!
        // register client
        clientCounts.put(clientName,0);

        // register team
        clientTeams.put(clientName,teamName);
        List<String> listOfTeamMembers;
        if(!teamClients.containsKey(teamName)) {
            // create a new team
            listOfTeamMembers = new ArrayList<String>();
            teamCounts.put(teamName,0);
        }
        else {
            // add the client to an existing team
            listOfTeamMembers = teamClients.get(teamName);
        }
        listOfTeamMembers.add(clientName);
        teamClients.put(teamName, listOfTeamMembers);

        return true;
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

                    //If no message sent from client, this code will block the program
                    String messageFromClient = dataInputStream.readUTF();

                    // parse message
                    String[] splitMessage = messageFromClient.split(":");
                    String clientName = splitMessage[0];
                    String teamName = splitMessage[1];
                    String messageCode = splitMessage[2];
                    String msg = splitMessage[3];

                    // register client if needed
                    if (!clientCounts.containsKey(clientName)) {
                        registerNewClient(clientName, teamName);

                        System.out.println("[SERVER] A new client (" + clientName + ") has registered in team " + teamName);
                        dataOutputStream.writeUTF(MsgClient.CONFIRM_REGISTRATION + ":" + "Welcome to the IceAge Nut Discovery game!\n" +
                                "You have enrolled as " + clientName + " in team " + teamName + ".\n" +
                                "Your team members are: " + teamClients.get(teamName).toString());
                    }

                    String printMessage = "";
                    String reply;

                    // handle message from client
                    switch (MsgServer.valueOf(messageCode)) {
                        //Client picked up an acorn, add the picture-name to the excluded list
                        //TODO add zones!!
                        case ACORN_PICKUP: // pickup acorn
                            if (clientPickUpAcorn(clientName, teamName, msg)) {
                                printMessage = "[SERVER] " + clientName + " picked up an acorn\n" +
                                        "~~~~~~ " + clientName + ": " + clientCounts.get(clientName) + ";" +
                                        " total count: " + totalNbPickedUp + "; total left: " + (totalNbAcorns - totalNbPickedUp) + ")"
                                        + getLeaderBoardString();
                                reply = MsgClient.CONFIRM_PICKUP + ":" + "You have picked up an acorn! \n" +
                                        totalNbPickedUp + " of the " + totalNbAcorns + " acorns are found";
                            } else {
                                printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up acorn," +
                                        "but this failed (client was not registered or acorn was not there).";
                                reply = MsgClient.DECLINE_PICKUP + ":" + "Oops, something went wrong, you were not able to pick up the acorn.";
                            }
                            break;

                        case DEFAULT: // other messages
                            //TODO nothing happens with these messages!
                            printMessage = "[SERVER] " + clientName + ": " + msg;
                            reply = MsgClient.TOAST + ":" + "Message received";
                            break;

                        case ENTER_ZONE: // enter a new zone
                            //TODO entering new zone, update excluded list on client
                            printMessage = "[SERVER] " + clientName + " entered a new zone (" + msg + ")";
                            reply = MsgClient.UPDATE_EXCLUDE_LIST + ":" + msg;
                            break;

                        case ACORN_REQUEST: //Check if the asked picture is in the excluded list
                            if (clientRequestAcorn(msg)) {
                                reply = MsgClient.CONFIRM_ACORN + ":" + msg;
//                                printMessage = "[SERVER] " + clientName + " requested acorn (" + msg + ")" +
//                                        " and it is free!";
                            } else {
                                reply = MsgClient.DECLINE_ACORN + ":" + msg;
//                                printMessage = "[SERVER] " + clientName + " requested acorn (" + msg + ")" +
//                                        ", but it has been taken";
                            }

                            break;

                        default:
                            printMessage = "[SERVER] ERROR - Something went wrong, invalid messagecode";
                            reply = "Oops, something went wrong";
                            break;

                    }
                    if(!printMessage.equals("")) {
                        System.out.println(printMessage);
                    }
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
            try {
                Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                        .getNetworkInterfaces();
                while (enumNetworkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = enumNetworkInterfaces
                            .nextElement();
                    Enumeration<InetAddress> enumInetAddress = networkInterface
                            .getInetAddresses();
                    while (enumInetAddress.hasMoreElements()) {
                        InetAddress inetAddress = enumInetAddress.nextElement();

                        if (inetAddress.isSiteLocalAddress()) {
                            ip += "SiteLocalAddress: "
                                    + inetAddress.getHostAddress() + "\n";
                        }

                    }

                }

            } catch (SocketException e) {
                e.printStackTrace();
                ip += "Something Wrong! " + e.toString() + "\n";
            }
            return ip;
        }
    }

}
