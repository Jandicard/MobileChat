import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE = "server_log.txt";

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        log("Servidor ligado.");

        while (true) {
            Socket socket = server.accept();
            logConn(socket.getInetAddress().getHostAddress());
            new Thread(new ClientHandler(socket)).start();
        }
    }

    public static void broadcast(String from, String to, String msg) {
        ClientHandler target = clients.get(to);
        if (target != null) {
            target.sendMessage("/message " + from + " " + msg);
        } else {
            ClientHandler sender = clients.get(from);
            if (sender != null)
                sender.sendMessage("Usuário não encontrado.");
        }
    }

    public static void sendFile(String from, String to, String filename, byte[] data) {
        ClientHandler target = clients.get(to);
        if (target != null) {
            target.sendFile(from, filename, data);
            log("Arquivo: " + from + " -> " + to + " (" + filename + ")");
        } else {
            ClientHandler sender = clients.get(from);
            if (sender != null)
                sender.sendMessage("Usuário não encontrado.");
        }
    }

    public static void addClient(String name, ClientHandler handler) {
        if (clients.containsKey(name)) {
            handler.sendMessage("Nome em uso.");
            try {
                handler.socket.close();
            } catch (IOException ignored) {
            }
            return;
        }
        clients.put(name, handler);
        updateUserList();
        log("Conectado: " + name);
    }

    public static void removeClient(String name) {
        clients.remove(name);
        updateUserList();
        log("Desconectado: " + name);
    }

    public static Set<String> getConnectedUsers() {
        return clients.keySet();
    }

    private static void updateUserList() {
        StringBuilder list = new StringBuilder("/users");
        for (String u : clients.keySet())
            list.append(" ").append(u);
        for (ClientHandler c : clients.values())
            c.sendMessage(list.toString());
    }

    private static void logConn(String ip) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println("Conexão: " + ip + " - " + sdf.format(new Date()));
        } catch (IOException ignored) {
        }
    }

    private static void log(String msg) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(sdf.format(new Date()) + " - " + msg);
        } catch (IOException ignored) {
        }
    }
}
