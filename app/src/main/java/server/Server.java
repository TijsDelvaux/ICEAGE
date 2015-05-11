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
    private Map<String, Socket> clientSockets = new HashMap<String, Socket>();

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
    public int clientPickUpAcorn(String clientName, String teamName, String imageName) {
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
                            notifyOfPickup(client, clientName);
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
    public boolean clientSetTrap(String clientName, String imageName) {
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
        int nbAcornsToTransfer = Math.min(clientCounts.get(clientName), costOfRunningInTrap);

        // change local counts
        clientCounts.put(clientName, clientCounts.get(clientName) - nbAcornsToTransfer);
        clientCounts.put(trapOwner, clientCounts.get(trapOwner) + nbAcornsToTransfer);
        updateTeamCount(clientTeams.get(clientName));
        updateTeamCount(clientTeams.get(trapOwner));

        // notify trapOwner
        System.out.println(clientName + " walked into the trap of " + trapOwner);
        sendMessageToClient(trapOwner, MsgClient.TRAP_REWARD,
                "Someone walked into your trap!\nYou receive " + nbAcornsToTransfer + " acorns\nThe trap has been dismantled"
                        + ":" + clientCounts.get(trapOwner)
                        + ":" + teamCounts.get(clientTeams.get(trapOwner))
                        + ":" + imageName);

        // notify all team players (both from the client as from the trapOwner)
        for(String client: teamClients.get(clientTeams.get(clientName))) {
            // make sure you do not notify yourself
            if(!client.equals(clientName)) {
                notifyOfTrapLoss(client,clientName, nbAcornsToTransfer);
            }
        }
        for(String client: teamClients.get(clientTeams.get(trapOwner))) {
            // make sure you do not notify yourself
            if(!client.equals(trapOwner)) {
                notifyOfTrapReward(client, trapOwner, nbAcornsToTransfer);
            }
        }

        // remove the trap after it is used
        trapMap.remove(imageName);
        return "You've walked into a trap!\nYou loose " + nbAcornsToTransfer + " acorns..."
                + ":" + clientCounts.get(clientName)
                + ":" + teamCounts.get(clientTeams.get(clientName));
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
    public boolean isThereAnAcorn(String imageName) {
        return !excludedMap.containsKey(imageName);
    }

    /*
     * @return: true if the trap is there, false if it is not
     */
    public boolean isThereATrap(String imageName) {
        return trapMap.containsKey(imageName);
    }

    /*
     * @return: a pretty String of the current leaderboard
     */
    public String getLeaderBoardString() {
        String indent = "    ";
        String leaderboard = "\n" + indent + "LEADERBOARD USERS\n";
        for(String name: clientCounts.keySet()) {
            leaderboard += indent + name + "\t\t" + clientCounts.get(name) + "\n";
        }
        leaderboard += "\n";
        leaderboard += "\n" + indent + "LEADERBOARD TEAMS\n";
        for(String name : teamCounts.keySet()) {
            leaderboard += indent + name + "\t\t" + teamCounts.get(name) + "\n";
        }
        leaderboard += "\n";
        return leaderboard;
    }

    public void notifyOfPickup(String clientName, String teamMate) {
        sendMessageToClient(clientName, MsgClient.TEAMMATE_PICKUP,
                                                    "Your teammate "+ teamMate + " picked up an acorn!"
                                                    + ":" + teamCounts.get(clientTeams.get(clientName)));

    }

    public void notifyOfTrapLoss(String clientName, String teamMate, int nbAcornsToTransfer) {
        sendMessageToClient(clientName, MsgClient.TEAMMATE_TRAP_REWARD,
                                                "Your teammate "+ teamMate + " walked into a trap!\n"
                                                        + "Your team lost " + nbAcornsToTransfer + " acorns."
                                                + ":" + teamCounts.get(clientTeams.get(clientName)));
    }

    public void notifyOfTrapReward(String clientName, String teamMate, int nbAcornsToTransfer) {
        sendMessageToClient(clientName, MsgClient.TEAMMATE_TRAP_REWARD,
                                                "Someone walked into a trap of your teammate" + teamMate + "!\n"
                                                        + "Your team gained " + nbAcornsToTransfer + " acorns."
                                                + ":" + teamCounts.get(clientTeams.get(clientName)));
    }

    /*
     * @return: true if the registration succeeded
     */
    public boolean registerNewClient(String clientName, String teamName){
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
        System.out.println("[SERVER] message toevoegen bij " + client + ": " + userMessage);
    }

    public ArrayList<String> getAllClients(){
        ArrayList<String> clients = new ArrayList<String>();
        for(List<String> clientsInTeam : teamClients.values()){
            clients.addAll(clientsInTeam);
        }
        return clients;
    }



    private class SocketServerThread extends Thread {

        private int SocketServerPORT = port;
        private Map<String,ClientConnection> clientConnectionMap = new HashMap<String, ClientConnection>();

        @Override
        public void run() {
            // listens to new client connections
            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                System.out.println("I'm waiting here: " + serverSocket.getLocalPort());
                System.out.println("IP: " + getIpAddress());
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("nieuwe connectie");
                    if(!socket.isClosed()) {
                        (new ClientConnection(socket)).start();
                    }

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



    private class ClientConnection extends Thread {
        private Socket clientSocket;
        private String clientName;
        private boolean loop = true;
        Stack<String> responses;
        ResponseGetter responseGetter;

        protected ClientConnection(Socket clientSocket) {
            System.out.println("[SERVER]: nieuwe clientconnection " + clientSocket.toString());
            this.clientSocket = clientSocket;
            this.responses = new Stack<String>();
            this.responseGetter = new ResponseGetter(this.clientSocket, this.responses);
            this.responseGetter.start();
        }

        public void setClientSocket(Socket socket){
            this.responseGetter.interrupt();
            this.clientSocket = socket;
            this.responseGetter = new ResponseGetter(this.clientSocket, this.responses);
            this.responseGetter.start();
        }

        public void setLoop(boolean loop){
            this.loop = loop;
        }

        public void run() {
            while (loop) {
                try {
                    DataOutputStream dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
                    if (msgsToClients.get(this.clientName) != null) {
                        while (!msgsToClients.get(this.clientName).empty()) {
                            dataOutputStream.writeUTF(msgsToClients.get(this.clientName).pop());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //If no message sent from client, this code will block the program
                while(!responses.empty()){
                    handleMessage(responses.pop());
                }
            }
        }

        private void handleMessage(String messageForClient) {
            // parse message
            String[] splitMessage = messageForClient.split(":");
            String clientName = splitMessage[0];
            String teamName = splitMessage[1];
            String messageCode = splitMessage[2];
            String msg = splitMessage[3];
            String printMessage = "";
            String reply = null;


            // handle message from client
            MsgClient code = null;
            switch (MsgServer.valueOf(messageCode)) {
                case REGISTER:
                    // register client if needed, beter met een register message?
                    if (!clientCounts.containsKey(clientName)) {
                        this.clientName = clientName;
                        clientSockets.put(clientName, clientSocket);
                        registerNewClient(clientName, teamName);
                        msgsToClients.put(clientName, new Stack<String>());
                        System.out.println("[SERVER] A new client (" + clientName + ") has registered in team " + teamName);
                        code = MsgClient.CONFIRM_REGISTRATION;
                        reply = "Welcome to the IceAge Nut Discovery game!\n" +
                                    "Name - " + clientName + "\n" +
                                    "Team -  " + teamName + "\n" +
                                    "Team members - " + teamClients.get(teamName).toString()
                                + ":" + "0"
                                + ":" + "0";
//                        sendMessageToClient(clientName, MsgClient.CONFIRM_REGISTRATION, reply);
                    } else {
                        // dit kan mss niet meer werken, omdat oude socket nog aan client is gekoppeld
                        // oplossing is dan dat je de oude met de nieuwe socket vervangt in clientSockets
                        try {
                            clientSockets.get(clientName).close();
                        } catch (IOException e) {
//                            e.printStackTrace();
                        }
                        clientSockets.put(clientName, clientSocket);
                        System.out.println("[SERVER] Client (" + clientName + ") has rejoined us!");
                        code = MsgClient.CONFIRM_REGISTRATION;
                        reply = "Welcome back!\n" +
                                    "Name - " + clientName + "\n" +
                                    "Team -  " + teamName + "\n" +
                                    "Team members - " + teamClients.get(teamName).toString()
                                + ":" + clientCounts.get(clientName)
                                + ":" + teamCounts.get(clientTeams.get(clientName));
//                        sendMessageToClient(clientName, MsgClient.CONFIRM_REGISTRATION, reply);
                    }
                    break;
                //Client picked up an acorn, add the picture-name to the excluded list
                //TODO add zones!!
                case ACORN_PICKUP: // pickup acorn

                    switch (clientPickUpAcorn(clientName, teamName, msg)) {
                        case CLIENT_CAN_PICK_UP_ACORN:
                            printMessage = "[SERVER] " + clientName + " picked up an acorn\n" +
                                    "~~~~~~ " + clientName + ": " + clientCounts.get(clientName) + ";" +
                                    " total count: " + totalNbPickedUp + "; total left: " + (totalNbAcorns - totalNbPickedUp) + ")"
                                    + getLeaderBoardString();
                            code = MsgClient.CONFIRM_PICKUP; //0
                            reply = "You have picked up an acorn! \n" +
                                    totalNbPickedUp + " of the " + totalNbAcorns + " acorns are found" //1
                                    + ":" + msg //2
                                    + ":" + clientCounts.get(clientName) //3
                                    + ":" + teamCounts.get(teamName);    //4
                            break;
                        case CLIENT_OWNS_THIS_ACORN:
                            printMessage = "[SERVER] " + clientName + " this acorn is already yours";
                            code = MsgClient.YOU_OWN_THIS_ACORN;//0
                            reply = msg //1
                                    + ":" + clientCounts.get(clientName) //2
                                    + ":" + teamCounts.get(teamName);    //3
                            break;
                        case ACORN_OWNED_BY_SOMEONE_ELSE:
                            printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up acorn," +
                                    "but this failed (acorn was not there).";
                            code = MsgClient.DECLINE_PICKUP;
                            reply = "Oops, something went wrong, you were not able to pick up the acorn."
                                    + ":" + msg;
                            System.out.println("PICKING UP ACORN - Client not registered");
                            break;
                        case CLIENT_IN_WRONG_TEAM:
                            printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up acorn," +
                                    "but this failed (client in the wrong team).";
                            code = MsgClient.DECLINE_PICKUP;
                            reply = "Oops, something went wrong, you were not able to pick up the acorn."
                                    + ":" + msg;
                            break;
                        case CLIENT_NOT_REGISTERED:
                            printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up acorn," +
                                    "but this failed (client was not registered).";
                            code = MsgClient.DECLINE_PICKUP;
                            reply = "Oops, something went wrong, you were not able to pick up the acorn."
                                    + ":" + msg;
                            break;
                        default:
                            printMessage = "[SERVER] ERROR - " + clientName + " tried to pick up acorn," +
                                    "but this failed (no idea what happend).";
                            code = MsgClient.DECLINE_PICKUP;
                            reply = "Oops, something went wrong, you were not able to pick up the acorn."
                                    + ":" + msg;
                            break;
                    }

                    break;

                case DEFAULT: // other messages
                    //TODO nothing happens with these messages!
                    printMessage = "[SERVER] " + clientName + ": " + msg;
                    code = MsgClient.TOAST;
                    reply = "Message received";
                    break;

                case ENTER_ZONE: // enter a new zone
                    //TODO entering new zone, update excluded list on client
                    printMessage = "[SERVER] " + clientName + " entered a new zone (" + msg + ")";
                    code =  MsgClient.UPDATE_EXCLUDE_LIST;
                    reply = msg;
                    break;

                case ACORN_REQUEST: //Check if the asked picture is in the excluded list
                    if (isThereATrap(msg)) {
                        printMessage = "[SERVER] " + clientName + " stepped in a trap!";
                        code = MsgClient.TRAP_LOSS;
                        reply = clientRunInTrap(clientName, msg);
                    } else if (isThereAnAcorn(msg)) {
                        code = MsgClient.CONFIRM_ACORN;
                        reply = msg;
                        printMessage = "[SERVER] " + clientName + " requested acorn (" + msg + ")" +
                                " and it is free!";
                    } else {
                        if (excludedMap.get(msg).equals(clientName)) {
                            printMessage = "[SERVER] " + clientName + " this acorn is already yours";
                            code = MsgClient.YOU_OWN_THIS_ACORN; //0
                            reply = msg //1
                                    + ":" + clientCounts.get(clientName) //2
                                    + ":" + teamCounts.get(teamName);    //3
                        } else {
                            code = MsgClient.DECLINE_ACORN;
                            reply = msg;
                            printMessage = "[SERVER] " + clientName + " requested acorn (" + msg + ")" +
                                    ", but it has been taken";
                        }
                    }
                    break;

                case SET_TRAP: //Place a new trap
                    if (clientSetTrap(clientName, msg)) {
                        code = MsgClient.CONFIRM_PLACEMENT_TRAP;
                        reply =  "You have successfully placed a trap!\nYou will be notified when someone walks into your trap."
                                + ":" + clientCounts.get(clientName) //2
                                + ":" + teamCounts.get(teamName);    //3
                        printMessage = "[SERVER] " + clientName + " has placed a trap";
                    } else {
                        code = MsgClient.DECLINE_PLACEMENT_TRAP;
                        reply =  msg;
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
            if(reply != null) {
                sendMessageToClient(clientName, code, reply);
            }

        }

        public class ResponseGetter extends Thread{
            Socket socket;

            public ResponseGetter(Socket socket, Stack<String> responses){
                this.socket = socket;
            }

            @Override
            public void run() {
                DataInputStream dataInputStream = null;
                while(true) {
                    try {
                        dataInputStream = new DataInputStream(this.socket.getInputStream());
                        String response = dataInputStream.readUTF();
                        System.out.println("response: " + response);
                        responses.push(response);
                    } catch (IOException e) {
//                    e.printStackTrace();
                    }
                }
            }


    }

}
}
