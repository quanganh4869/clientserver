import javax.swing.*;
import java.awt.BorderLayout;
import java.io.*;
import java.net.*;

public class MailServer extends JFrame {
    private JTextArea serverLog;
    private static final String BASE_DIR = "MailServerAccounts";
    private ServerSocket serverSocket;

    public static void main(String[] args) {
        new MailServer().startServer();
    }

    public MailServer() {
        setTitle("Mail Server");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        serverLog = new JTextArea();
        serverLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(serverLog);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    public void logMessage(String message) {
        serverLog.append(message + "\n");
    }

    // Thêm phương thức hiển thị thông báo
    public void showNotification(String message) {
        JOptionPane.showMessageDialog(this, message, "Notification", JOptionPane.INFORMATION_MESSAGE);
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(8888);
            logMessage("Server is running on port 8888...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logMessage("Client connected: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String currentUser; // Lưu tên người dùng hiện tại

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processCommand(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    if (currentUser != null) {
                        logMessage("User logged out: " + currentUser);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Xử lý các lệnh từ client
        private void processCommand(String command) {
            String[] parts = command.split(" ", 3);
            String action = parts[0];

            switch (action) {
                case "REGISTER":
                    handleRegister(parts[1], parts[2]);
                    break;
                case "LOGIN":
                    currentUser = parts[1];
                    logMessage("User logged in: " + currentUser);
                    break;
                case "LOGOUT":
                    logMessage("User logged out: " + currentUser);
                    currentUser = null;
                    break;
                case "SEND_EMAIL":
                    String sender = parts[1];
                    String recipient = parts[2];
                    StringBuilder emailContent = new StringBuilder();
                    String line;
                    try {
                        while ((line = in.readLine()) != null && !line.isEmpty()) {
                            emailContent.append(line).append("\n");
                        }
                        saveEmail(sender, recipient, emailContent.toString());
                        logMessage("Message sent from " + sender + " to " + recipient);
                        // Hiển thị thông báo khi email được gửi thành công
                        showNotification("Email sent from " + sender + " to " + recipient);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "NOTIFY":
                    handleNotification(parts[1], parts[2]);
                    break;
                default:
                    logMessage("Unknown command: " + command);
            }
        }

        // Đăng ký tài khoản
        private void handleRegister(String accountName, String password) {
            File userDir = new File(BASE_DIR + "/" + accountName);
            if (!userDir.exists()) {
                userDir.mkdirs();
                logMessage("Account created: " + accountName);
            }
        }

        // Xử lý thông báo từ client
        private void handleNotification(String sender, String recipient) {
            logMessage(sender + " sent an email to " + recipient);
            // Tạo thư mục nếu chưa tồn tại
            File userDir = new File(BASE_DIR + "/" + recipient);
            if (!userDir.exists()) {
                userDir.mkdirs();
                logMessage("Created directory for recipient: " + recipient);
            }
        }

        private int getNextMailIndex(String recipient) {
            File recipientDir = new File(BASE_DIR + "/" + recipient);
            if (recipientDir.exists() && recipientDir.isDirectory()) {
                File[] mailFiles = recipientDir.listFiles((dir, name) -> name.startsWith("mail"));
                return mailFiles != null ? mailFiles.length + 1 : 1;
            }
            return 1;
        }

        // Lưu email
        private void saveEmail(String sender, String recipient, String emailContent) {
            File recipientDir = new File(BASE_DIR + "/" + recipient);
            if (!recipientDir.exists()) {
                logMessage("Recipient does not exist: " + recipient);
                return;
            }

            int mailIndex = getNextMailIndex(recipient);
            File mailFile = new File(recipientDir, "mail_" + mailIndex + ".txt");

            try (PrintWriter pw = new PrintWriter(new FileWriter(mailFile))) {
                pw.println("From: " + sender);
                pw.println("To: " + recipient);
                pw.println("Content:");
                pw.println(emailContent);
                logMessage("Email saved to " + recipient + "'s mailbox as " + mailFile.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
