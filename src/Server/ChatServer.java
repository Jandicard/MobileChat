import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE = "server_log.txt";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serverSocket.close();
                System.out.println("Servidor encerrado.");
            } catch (IOException e) {
                System.err.println("Erro ao fechar o servidor: " + e.getMessage());
            }
        }));
        System.out.println("Servidor iniciado na porta " + PORT);
        logServer("Servidor iniciado");
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                String clientAddress = socket.getInetAddress().getHostAddress();
                logConnection(clientAddress);
                System.out.println("Novo cliente conectado: " + clientAddress);

                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    public static void broadcast(String sender, String receiver, String message) {
        ClientHandler recipient = clients.get(receiver);
        if (recipient != null) {
            recipient.sendMessage("/message " + sender + " " + message);
        }
    }

    public static void sendFile(String sender, String receiver, String filename, byte[] fileData) {
        ClientHandler recipient = clients.get(receiver);
        if (recipient != null) {
            recipient.sendFile(sender, filename, fileData);
        }
    }

    public static void addClient(String name, ClientHandler handler) {
        clients.put(name, handler);
        broadcastUserList();
    }

    public static void removeClient(String name) {
        clients.remove(name);
        broadcastUserList();
        System.out.println(name + " desconectado");
    }

    public static Set<String> getConnectedUsers() {
        return clients.keySet();
    }

    private static void broadcastUserList() {
        StringBuilder userList = new StringBuilder("/users");
        for (String username : clients.keySet()) {
            userList.append(" ").append(username);
        }

        for (ClientHandler client : clients.values()) {
            client.sendMessage(userList.toString());
        }
    }

    private static void logConnection(String clientAddress) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println("Cliente conectado: " + clientAddress + " - " + dateFormat.format(new Date()));
        } catch (IOException e) {
            System.err.println("Erro ao registrar conexão no log: " + e.getMessage());
        }
    }

    private static void logServer(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(dateFormat.format(new Date()) + " - " + message);
        } catch (IOException e) {
            System.err.println("Erro ao registrar mensagem no log: " + e.getMessage());
        }
    }
}