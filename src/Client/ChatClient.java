import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Socket socket = new Socket(SERVER_IP, SERVER_PORT);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        System.out.print("Digite seu nome de usuário: ");
        String name = scanner.nextLine();
        out.writeUTF(name);

        new Thread(() -> {
            try {
                while (true) {
                    String messageType = in.readUTF();

                    if (messageType.equals("/text")) {
                        String msg = in.readUTF();

                        if (msg.startsWith("/message ")) {
                            String[] parts = msg.split(" ", 3);
                            if (parts.length >= 3) {
                                System.out.println(parts[1] + ": " + parts[2]);
                            } else {
                                System.out.println(msg);
                            }
                        } else if (msg.startsWith("/users")) {
                            String[] userList = msg.split(" ");
                            System.out.println("\nUsuários conectados:");
                            for (int i = 1; i < userList.length; i++) {
                                System.out.println("- " + userList[i]);
                            }
                            System.out.println();
                        } else {
                            System.out.println(msg);
                        }
                    } else if (messageType.equals("/file")) {
                        String sender = in.readUTF();
                        String filename = in.readUTF();
                        int fileSize = in.readInt();
                        byte[] fileData = new byte[fileSize];
                        in.readFully(fileData);

                        File receivedFile = new File(filename);
                        Files.write(receivedFile.toPath(), fileData);
                        System.out.println(sender + " enviou o arquivo: " + filename + " (" + fileSize + " bytes)");
                        System.out.println("Arquivo salvo em: " + receivedFile.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                System.out.println("Conexão encerrada.");
                System.exit(0);
            }
        }).start();

        while (true) {
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("/sair")) {
                out.writeUTF("/text");
                out.writeUTF(input);
                break;
            } else if (input.equals("/users")) {
                out.writeUTF("/text");
                out.writeUTF(input);
            } else if (input.startsWith("/send message")) {
                out.writeUTF("/text");
                out.writeUTF(input);
            } else if (input.startsWith("/send file")) {
                String[] parts = input.split(" ", 4);
                if (parts.length < 4) {
                    System.out.println("Formato inválido. Use: /send file <destinatario> <caminho do arquivo>");
                    continue;
                }

                String receiver = parts[2];
                String filePath = parts[3];
                handleFileSend(out, receiver, filePath);
            } else {
                System.out.println("Comando não reconhecido. Use /users, /send message, /send file ou /sair");
            }
        }

        socket.close();
        scanner.close();
    }

    private static void handleFileSend(DataOutputStream out, String receiver, String filePath) {
        try {
            File file = new File(filePath);

            if (!file.exists()) {
                System.out.println("Arquivo não encontrado: " + filePath);
                return;
            }

            byte[] fileData = Files.readAllBytes(file.toPath());
            String filename = file.getName();
            out.writeUTF("/file");
            out.writeUTF(receiver);
            out.writeUTF(filename);
            out.writeInt(fileData.length);
            out.write(fileData);
            out.flush();

            System.out.println("Arquivo " + filename + " (" + fileData.length + " bytes) enviado para " + receiver);
        } catch (IOException e) {
            System.err.println("Erro ao enviar o arquivo: " + e.getMessage());
        }
    }
}