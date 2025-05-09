import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                        System.out.println(msg);
                    } else if (messageType.equals("/file")) {
                        String sender = in.readUTF();
                        String filename = in.readUTF();
                        int fileSize = in.readInt();
                        byte[] fileData = new byte[fileSize];
                        in.readFully(fileData);

                        File baseDir = new File(System.getProperty("user.dir"));
                        File targetDir = new File(baseDir.getParentFile(), "src/RecebeArquivo");
                        if (!targetDir.exists()) {
                            targetDir.mkdirs();
                        }

                        File receivedFile = new File(targetDir, filename);
                        Files.write(receivedFile.toPath(), fileData);
                        System.out.println(sender + " enviou o arquivo: " + filename + " (salvo em "
                                + receivedFile.getAbsolutePath() + ")");
                    } else if (messageType.equals("/users")) {
                        String users = in.readUTF();
                        System.out.println("\nUsuários conectados:");
                        String[] userList = users.split(";");
                        for (String user : userList) {
                            if (!user.isEmpty()) {
                                System.out.println("- " + user);
                            }
                        }
                        System.out.println();
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
                handleFileSend(out, input);
            } else {
                out.writeUTF("/text");
                out.writeUTF("Comando não reconhecido.");
            }
        }

        socket.close();
        scanner.close();
    }

    private static void handleFileSend(DataOutputStream out, String input) throws IOException {
        Pattern pattern = Pattern.compile("^/send file (\\S+)\\s+(.+)$");
        Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            System.out.println("Formato inválido. Use: /send file <destinatario> <caminho do arquivo>");
            return;
        }

        String receiver = matcher.group(1);
        String filePath = matcher.group(2);
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Arquivo não encontrado: " + filePath);
            return;
        }

        try {
            byte[] fileData = Files.readAllBytes(file.toPath());
            out.writeUTF("/file");
            out.writeUTF(receiver);
            out.writeUTF(file.getName());
            out.writeInt(fileData.length);
            out.write(fileData);
            System.out.println("Arquivo " + file.getName() + " enviado para " + receiver + ".");
        } catch (IOException e) {
            System.out.println("Erro ao ler/enviar arquivo: " + e.getMessage());
        }
    }
}
