import java.io.*;
import java.net.Socket;

class ClientHandler implements Runnable {
    public Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public void run() {
        try {
            username = in.readUTF();
            ChatServer.addClient(username, this);
            System.out.println("Cliente " + username + " registrado com sucesso.");

            while (true) {
                String messageType = in.readUTF();

                if (messageType.equals("/text")) {
                    String message = in.readUTF();
                    System.out.println("Recebido de " + username + ": " + message);

                    if (message.startsWith("/send message")) {
                        handleSendMessage(message);
                    } else if (message.equals("/users")) {
                        sendUserList();
                    } else if (message.equals("/sair")) {
                        System.out.println("Cliente " + username + " solicitou desconexão.");
                        break;
                    }
                } else if (messageType.equals("/file")) {
                    handleFileTransfer();
                }
            }
        } catch (IOException e) {
            System.out.println("Erro com cliente " + username + ": " + e.getMessage());
        } finally {
            ChatServer.removeClient(username);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void sendUserList() {
        StringBuilder userList = new StringBuilder("/users");
        for (String user : ChatServer.getConnectedUsers()) {
            userList.append(" ").append(user);
        }
        sendMessage(userList.toString());
    }

    private void handleSendMessage(String message) throws IOException {
        String[] parts = message.split(" ", 4);
        if (parts.length < 4) {
            sendMessage("Formato inválido. Use: /send message <destinatario> <mensagem>");
            return;
        }

        String receiver = parts[2];
        String msgContent = parts[3];
        ChatServer.broadcast(username, receiver, msgContent);
        sendMessage("Mensagem enviada para " + receiver + ".");
    }

    private void handleFileTransfer() throws IOException {
        try {
            String receiver = in.readUTF();
            String filename = in.readUTF();
            int fileSize = in.readInt();

            System.out.println("Transferência de arquivo iniciada: " + username + " -> " + receiver + " (" + filename
                    + ", " + fileSize + " bytes)");

            byte[] fileData = new byte[fileSize];
            in.readFully(fileData);

            ChatServer.sendFile(username, receiver, filename, fileData);
            sendMessage("Arquivo " + filename + " enviado para " + receiver + ".");

            System.out.println(
                    "Transferência de arquivo concluída: " + username + " -> " + receiver + " (" + filename + ")");
        } catch (IOException e) {
            System.err.println("Erro na transferência de arquivo: " + e.getMessage());
            sendMessage("Erro ao processar o arquivo. Por favor, tente novamente.");
            throw e;
        }
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF("/text");
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem para " + username + ": " + e.getMessage());
        }
    }

    public void sendFile(String sender, String filename, byte[] fileData) {
        try {
            out.writeUTF("/file");
            out.writeUTF(sender);
            out.writeUTF(filename);
            out.writeInt(fileData.length);
            out.write(fileData);
            out.flush();
            System.out.println("Arquivo " + filename + " encaminhado de " + sender + " para " + username);
        } catch (IOException e) {
            System.err.println("Erro ao enviar arquivo para " + username + ": " + e.getMessage());
        }
    }
}