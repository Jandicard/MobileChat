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

        System.out.print("Usuário: ");
        String name = scanner.nextLine();
        out.writeUTF(name);

        new Thread(() -> {
            try {
                while (true) {
                    String type = in.readUTF();

                    if (type.equals("/text")) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/message ")) {
                            String[] parts = msg.split(" ", 3);
                            if (parts.length >= 3) {
                                System.out.println(parts[1] + ": " + parts[2]);
                            }
                        } else if (msg.startsWith("/users")) {
                            String[] users = msg.split(" ");
                            System.out.println("Online:");
                            for (int i = 1; i < users.length; i++) {
                                System.out.println("- " + users[i]);
                            }
                        } else {
                            System.out.println(msg);
                        }
                    } else if (type.equals("/file")) {
                        String sender = in.readUTF();
                        String filename = in.readUTF();
                        int size = in.readInt();
                        byte[] data = new byte[size];
                        in.readFully(data);

                        Files.write(new File(filename).toPath(), data);
                        System.out.println(sender + " enviou um arquivo: " + filename);
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
            } else if (input.startsWith("/send message") || input.equals("/users")) {
                out.writeUTF("/text");
                out.writeUTF(input);
            } else if (input.startsWith("/send file")) {
                String[] parts = input.split(" ", 4);
                if (parts.length < 4) {
                    System.out.println("Use: /send file <destinatario> <arquivo>");
                    continue;
                }

                String receiver = parts[2];
                String path = parts[3];
                sendFile(out, receiver, path);
            } else {
                System.out.println("Comando inválido.");
            }
        }

        socket.close();
        scanner.close();
    }

    private static void sendFile(DataOutputStream out, String to, String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("Arquivo não encontrado.");
                return;
            }

            byte[] data = Files.readAllBytes(file.toPath());
            out.writeUTF("/file");
            out.writeUTF(to);
            out.writeUTF(file.getName());
            out.writeInt(data.length);
            out.write(data);
            out.flush();

            System.out.println("Arquivo enviado.");
        } catch (IOException e) {
            System.out.println("Erro ao enviar arquivo.");
        }
    }
}
