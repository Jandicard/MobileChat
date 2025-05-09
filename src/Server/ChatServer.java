import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE = "server_log.txt";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
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
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            logServer("Erro no servidor: " + e.getMessage());
        } finally {
            try {
                serverSocket.close();
                logServer("Servidor encerrado");
            } catch (IOException e) {
                System.err.println("Erro ao fechar servidor: " + e.getMessage());
            }
        }
    }

    public static void broadcast(String sender, String receiver, String message) {
        ClientHandler recipient = clients.get(receiver);
        if (recipient != null) {
            recipient.sendMessage("/message " + sender + " " + message);
            System.out.println("Mensagem encaminhada: " + sender + " -> " + receiver);
        } else {
            ClientHandler senderHandler = clients.get(sender);
            if (senderHandler != null) {
                senderHandler.sendMessage("Usuário " + receiver + " não encontrado ou não está conectado.");
            }
            System.out.println("Tentativa de envio para usuário inexistente: " + receiver);
        }
    }

    public static void sendFile(String sender, String receiver, String filename, byte[] fileData) {
        ClientHandler recipient = clients.get(receiver);
        if (recipient != null) {
            recipient.sendFile(sender, filename, fileData);
            System.out.println("Arquivo encaminhado: " + sender + " -> " + receiver + " (" + filename + ", "
                    + fileData.length + " bytes)");

            logServer("Transferência de arquivo: " + sender + " -> " + receiver + " (" + filename + ", "
                    + fileData.length + " bytes)");
        } else {
            ClientHandler senderHandler = clients.get(sender);
            if (senderHandler != null) {
                senderHandler.sendMessage("Usuário " + receiver + " não encontrado ou não está conectado.");
            }
            System.out.println("Tentativa de envio de arquivo para usuário inexistente: " + receiver);
        }
    }

    public static void addClient(String name, ClientHandler handler) {
        if (clients.containsKey(name)) {
            try {
                handler.sendMessage("Nome de usuário já em uso. Por favor, escolha outro nome.");
                handler.socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar conexão de usuário duplicado: " + e.getMessage());
            }
            return;
        }

        clients.put(name, handler);
        logServer("Cliente registrado: " + name);
        broadcastUserList();
    }

    public static void removeClient(String name) {
        clients.remove(name);
        broadcastUserList();
        logServer("Cliente desconectado: " + name);
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