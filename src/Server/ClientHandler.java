import java.io.*;
import java.net.Socket;

class ClientHandler implements Runnable {
    public Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String name;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public void run() {
        try {
            name = in.readUTF();
            ChatServer.addClient(name, this);

            while (true) {
                String type = in.readUTF();

                if (type.equals("/text")) {
                    String msg = in.readUTF();

                    if (msg.startsWith("/send message")) {
                        handleMessage(msg);
                    } else if (msg.equals("/users")) {
                        sendUserList();
                    } else if (msg.equals("/sair")) {
                        break;
                    }
                } else if (type.equals("/file")) {
                    handleFile();
                }
            }
        } catch (IOException ignored) {
        } finally {
            ChatServer.removeClient(name);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void sendUserList() {
        StringBuilder list = new StringBuilder("/users");
        for (String u : ChatServer.getConnectedUsers())
            list.append(" ").append(u);
        sendMessage(list.toString());
    }

    private void handleMessage(String msg) throws IOException {
        String[] parts = msg.split(" ", 4);
        if (parts.length < 4) {
            sendMessage("Use: /send message <dest> <mensagem>");
            return;
        }
        String to = parts[2];
        String text = parts[3];
        ChatServer.broadcast(name, to, text);
    }

    private void handleFile() throws IOException {
        String to = in.readUTF();
        String filename = in.readUTF();
        int size = in.readInt();
        byte[] data = new byte[size];
        in.readFully(data);

        ChatServer.sendFile(name, to, filename, data);
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF("/text");
            out.writeUTF(msg);
            out.flush();
        } catch (IOException ignored) {
        }
    }

    public void sendFile(String from, String filename, byte[] data) {
        try {
            out.writeUTF("/file");
            out.writeUTF(from);
            out.writeUTF(filename);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        } catch (IOException ignored) {
        }
    }
}
