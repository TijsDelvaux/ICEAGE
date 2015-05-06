package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
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
import java.util.Stack;

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
    private Map<String, Stack<String>> msgsToClients;

    private Map<String,String> excludedMap;
    private Map<String,String> trapMap;

    private int totalNbPickedUp;
    private final int totalNbAcorns = 56;
    private final int costOfSettingTrap = 1;
    private final int costOfRunningInTrap = 2;

    public static void main(String[] args) {
        new Server();
    }

    public Server(){
        totalNbPickedUp = 0;
        clientTeams = new HashMap<String, String>();
        teamClients = new HashMap<String, List<String>>();
        clientCounts = new HashMap<String, Integer>();
        teamCounts = new HashMap<String, Integer>();
        excludedMap = new HashMap<String,String>();
        trapMap = new HashMap<String, String>();
        msgsToClients = new HashMap<String, Stack<String>>();

        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }

    public void excludeImage(String image, String clientName){
        excludedMap.put(image, clientName);
    }

    private static final int CLIENT_CAN_PICK_UP_ACORN = 0;
    private static final int CLIENT_OWNS_THIS_ACORN = 1;
    private static final int ACORN_OWNED_BY_SOMEONE_ELSE = 2;
    private static final int CLIENT_IN_WRONG_TEAM = 3;
    private static final int CLIENT_NOT_REGISTERED = 4;

    /*
    * @return: true if the client was able to pickup the acorn
    */
    private int clientPickUpAcorn(String clientName, String teamName, String imageName) {
        if(clientCounts.containsKey(clientName)) {
            //Acorn not yet picked up
            if (!excludedMap.containsKey(imageName)) {
                // pickup
                excludeImage(imageName,clientName);
                clientCounts.put(clientName, clientCounts.get(clientName) + 1);
                updateTeamCount(teamName);
                totalNbPickedUp++;

                // notify all team players
                if (clientTeams.get(clientName).equals(teamName)) {
                    // find all teamplayers
                    for (String client : teamClients.get(teamName)) {
                        // make sure you do not notify yourself
                        if (!client.equals(clientName)) {
                            notifyOfPickup(clientName);
                        }
                    }
                } else {
                    // this client says that he is in an other team than the server thinks he is; this should not happen
                    return CLIENT_IN_WRONG_TEAM;
                }
                return CLIENT_CAN_PICK_UP_ACORN;
            } else if(excludedMap.get(imageName).equals(clientName)){
                return CLIENT_OWNS_THIS_ACORN;
            } else{
                return ACORN_OWNED_BY_SOMEONE_ELSE;
            }

        }else{
            // this client is not registered; this should not happen
            return CLIENT_NOT_REGISTERED;
        }
    }

    /*
    * @return: true if the client was able to place a trap here
    */
    private boolean clientSetTrap(String clientName, String imageName) {
        // a trap can only be placed by a registered user
        //                           on a spot where the acorn is taken by one of your teammates
        if(clientCounts.containsKey(clientName) && excludedMap.containsKey(imageName)) { //TODO check requirements
            // place the trap
            trapMap.put(imageName, clientName);

            clientCounts.put(clientName,clientCounts.get(clientName) - costOfSettingTrap);
            updateTeamCount(clientTeams.get(clientName));

            sendMessageToClient(clientName,MsgClient.CONFIRM_PLACEMENT_TRAP,"You have successfully placed a trap!\nYou will be notified when someone walks into your trap.");
            return true;
        }
        else {
            // this client cannot place a trap here
            return false;
        }
    }

    /*
    * @return: the message for the client who walked in the trap
    * @pre: there must be a trap here!
    */
    public String clientRunInTrap(String clientName, String imageName) {
        String trapOwner = trapMap.get(imageName);
        int nbAcornsToTransfer = Math.max(clientCounts.get(clientName),costOfRunningInTrap);

        // change local counts
        clientCounts.put(clientName,clientCounts.get(clientName) - nbAcornsToTransfer);
        clientCounts.put(trapOwner,clientCounts.get(trapOwner) + nbAcornsToTransfer);
        updateTeamCount(clientTeams.get(clientName));
        updateTeamCount(clientTeams.get(trapOwner));

        // notify trapOwner
        sendMessageToClient(trapOwner,MsgClient.TRAP_REWARD,
                nbAcornsToTransfer + ":Someone walked into your trap!\nYou receive " + nbAcornsToTransfer + " acorns...\nThe trap is now deleted");

        // notify all team players (both from the client as from the trapOwner)
        for(String client: teamClients.get(clientName)) {
            // make sure you do not notify yourself
            if(!client.equals(clientName)) {
                notifyOfTrapLoss(clientName, nbAcornsToTransfer);
            }
        }
        for(String client: teamClients.get(trapOwner)) {
            // make sure you do not notify yourself
            if(!client.equals(trapOwner)) {
                notifyOfTrapReward(trapOwner, nbAcornsToTransfer);
            }
        }

        // remove the trap after it is used
        trapMap.remove(imageName);

        return nbAcornsToTransfer + ":You've walked into a trap!\nYou loose " + nbAcornsToTransfer + " acorns...";
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
    private boolean isThereAnAcorn(String imageName) {
        return !excludedMap.containsKey(imageName);
    }

    /*
     * @return: true if the trap is there, false if it is not
     */
    private boolean isThereATrap(String imageName) {
        return trapMap.containsKey(imageName);
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
            leaderboard += indent + name + "\t\t" + teamCounts.get(name) + "\n";
        }
        leaderboard += "\n";
        return leaderboard;
    }

    private void notifyOfPickup(String clientName) {
        sendMessageToClient(clientName, MsgClient.TEAMMATE_PICKUP, ":A teammate picked up an achorn!");
    }

    private void notifyOfTrapLoss(String clientName, int nbAcornsToTransfer) {
        sendMessageToClient(clientName, MsgClient.TEAMMATE_TRAP_REWARD, ":" + nbAcornsToTransfer + ":A teammate walked into a trap!");
    }

    private void notifyOfTrapReward(String clientName, int nbAcornsToTransfer) {
        sendMessageToClient(clientName, MsgClient.TEAMMATE_TRAP_REWARD, ":" + nbAcornsToTransfer + ":Someone walked into a trap of your teammate!");
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

    public void sendMessageToClient(String client, MsgClient code, String message){
        String userMessage =  code + ":" + message;
        msgsToClients.get(client).push(userMessage);
        System.out.println("[SERVER] mesage toevoegen bij " + client + ": " + userMessage);
    }



    private class SocketServerThread extends Thread {

        private int SocketServerPORT = port;

        @Override
        public void run() {
            // listens to new client connections
            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                System.out.println("I'm waiting here: " + serverSocket.getLocalPort());
                System.out.println("IP: " + getIpAddress());
            while (true) {
                Socket socket = serverSocket.accept();
                (new ClientConnection(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                final String errMsg = e.toString();
                System.out.println(errMsg);
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



    private class ClientConnection extends Thread{
        private Socket clientSocket;
        private String clientName;

        protected ClientConnection(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        public void run(){
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            try {

                while (true) {
                    dataInputStream = new DataInputStream(this.clientSocket.getInputStream());
                    dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
                    if(msgsToClients.get(this.clientName) != null) {
                        while (!msgsToClients.get(this.clientName).empty()) {
                            dataOutputStream.writeUTF(msgsToClients.get(this.clientName).pop());
                        }
                    }

                    //If no message sent from client, this code will block the program
                    String messageFromClient = "";
                    try {
                        messageFromClient = dataInputStream.readUTF();
                    } catch (EOFException e) {
                        continue;
                    }

                    // parse message
                    String[] splitMessage = messageFromClient.split(":");
                    String clientName = splitMessage[0];
                    String teamName = splitMessage[1];
                    String messageCode = splitMessage[2];
                    String msg = splitMessage[3];
                    String printMessage = "";
                    String reply;

                    // register client if needed, beter met een register message?
                    this.clientName = clientName;
                    if (!clientCounts.containsKey(clientName)) {
                        registerNewClient(clientName, teamName);

                        System.out.println("[SERVER] A new client (" + clientName + ") has registered in team " + teamName);
                        dataOutputStream.writeUTF(MsgClient.CONFIRM_REGISTRATION + ":" + "Welcome to the IceAge Nut Discovery game!\n" +
                                "You have enrolled as " + clientName + " in team " + teamName + ".\n" +
                                "Your team members are " + teamClients.get(teamName).toString());
                        msgsToClients.put(this.clientName, new Stack<String>());
//                        dit was om te testen
//                        sendMessageToClient(this.clientName, MsgClient.TEAMMATE_PICKUP, "joepie");
                    }



                    // handle message from client
                    switch (MsgServer.valueOf(messageCode)) {
                        //Client picked up an acorn, add the picture-name to the excluded list
                        //TODO add zones!!
                        case ACORN_PICKUP: // pickup acorn

                            switch (clientPickUpAcorn(clientName, teamName, msg)){
                                case CLIENT_CAN_PICK_UP_ACORN:
                                    printMessage = "[SERVER] " + clientName + " picked up an acorn\n" +
                                            "~~~~~~ " + clientName + ": " + clientCounts.get(clientName) + ";" +
                                            " total count: " + totalNbPickedUp + "; total left: " + (totalNbAcorns - totalNbPickedUp) + ")"
                                            + getLeaderBoardString();
                                    reply = MsgClient.CONFIRM_PICKUP //0
                                            + ":" + "You have picked up an acorn! \n" +
                                            totalNbPickedUp + " of the " + totalNbAcorns + " acorns are found" //1
                                            + ":" + msg //2
                                            + ":" + clientCounts.get(clientName) //3
                                            + ":" + teamCounts.get(teamName);    //4
                                    break;
                                case CLIENT_OWNS_THIS_ACORN:
                                    printMessage = "[SERVER] " + clientName + " this acorn is already yours";
                                    reply = MsgClient.YOU_OWN_THIS_ACORN //0
                                            + ":" + msg //1
                                            + ":" + clientCounts.get(clientName) //2
                                            + ":" + teamCounts.get(teamName);    //3
                                    break;
                                case ACORN_OWNED_BY_SOMEONE_ELSE:
                                    printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up acorn," +
                                            "but this failed (acorn was not there).";
                                    reply = MsgClient.DECLINE_PICKUP + ":" + "Oops, something went wrong, you were not able to pick up the acorn."
                                            + ":" + msg;
                                    break;
                                case CLIENT_IN_WRONG_TEAM:
                                    printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up acorn," +
                                            "but this failed (client in the wrong team).";
                                    reply = MsgClient.DECLINE_PICKUP + ":" + "Oops, something went wrong, you were not able to pick up the acorn."
                                            + ":" + msg;
                                    break;
                                case CLIENT_NOT_REGISTERED:
                                    printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up acorn," +
                                            "but this failed (client was not registered).";
                                    reply = MsgClient.DECLINE_PICKUP + ":" + "Oops, something went wrong, you were not able to pick up the acorn."
                                            + ":" + msg;
                                    break;
                                default:
                                    printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up acorn," +
                                            "but this failed (no idea what happend).";
                                    reply = MsgClient.DECLINE_PICKUP + ":" + "Oops, something went wrong, you were not able to pick up the acorn."
                                            + ":" + msg;
                                    break;
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
                            if (isThereATrap(msg)) {
                                reply = clientRunInTrap(clientName, msg);
                                printMessage = "[SERVER] " + clientName + " stepped in a trap!";
                            }
                            else if (isThereAnAcorn(msg)) {
                                reply = MsgClient.CONFIRM_ACORN + ":" + msg;
                                printMessage = "[SERVER] " + clientName + " requested acorn (" + msg + ")" +
                                        " and it is free!";

                            } else {
                                if(excludedMap.get(msg).equals(clientName)){
                                    printMessage = "[SERVER] " + clientName + " this acorn is already yours";
                                    reply = MsgClient.YOU_OWN_THIS_ACORN //0
                                            + ":" + msg //1
                                            + ":" + clientCounts.get(clientName) //2
                                            + ":" + teamCounts.get(teamName);    //3
                                }else {
                                    reply = MsgClient.DECLINE_ACORN + ":" + msg;
                                    printMessage = "[SERVER] " + clientName + " requested acorn (" + msg + ")" +
                                            ", but it has been taken";
                                }
                            }

                            break;

                        case SET_TRAP: //Place a new trap
                            if (clientSetTrap(clientName, msg)) {
                                reply = MsgClient.CONFIRM_PLACEMENT_TRAP + ":" + costOfSettingTrap + ":You have successfully placed a trap!\nYou will be notified when someone walks into your trap.";
                                printMessage = "[SERVER] " + clientName + " has placed a trap";
                            }
                            else {
                                reply = MsgClient.DECLINE_PLACEMENT_TRAP + ":" + msg;
                                printMessage = "[SERVER] " + clientName + " wanted to place a trap, but it failed";
                            }
                            break;

                        default:
                            printMessage = "[SERVER] ERROR - Something went wrong, invalid messagecode";
                            reply = "Oops, something went wrong";
                            break;

                    }
                    if (!printMessage.equals("")) {
                        System.out.println(printMessage);
                    }
                    System.out.println("Reply: " + reply);
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
    }

}
