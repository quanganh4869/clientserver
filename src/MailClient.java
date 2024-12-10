import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class MailClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String serverAddress;

    public MailClient(String serverAddress) {
        this.serverAddress = serverAddress;
        connectToServer();
        showRegisterGUI();
    }

    public static void main(String[] args) {
        String serverIP = JOptionPane.showInputDialog("Enter Server IP Address:");
        new MailClient(serverIP);
    }

    // Kết nối tới server
    private void connectToServer() {
        try {
            socket = new Socket(serverAddress, 8888);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Unable to connect to server: " + e.getMessage());
            System.exit(1); // Kết thúc chương trình nếu không kết nối được
        }
    }

    // Hiển thị giao diện Đăng ký
    private void showRegisterGUI() {
        JFrame frame = createFrame("Register", 400, 300);
        frame.getContentPane().setBackground(new Color(240, 240, 240));
        JPanel panel = createPanel(4, 2);

        JTextField accountField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton registerButton = new JButton("Register");
        JButton loginButton = new JButton("Login");

        Font font = new Font("Arial", Font.BOLD, 14);
        registerButton.setFont(font);
        loginButton.setFont(font);

        // Xử lý nút Đăng ký
        registerButton.addActionListener(e -> handleRegister(accountField, passwordField, frame));
        // Xử lý nút Đăng nhập
        loginButton.addActionListener(e -> {
            frame.dispose();
            showLoginGUI(null);
        });

        addComponentsToPanel(panel, new JLabel("Account Name:"), accountField,
                new JLabel("Password:"), passwordField, registerButton, loginButton);
        

        frame.add(panel);
        frame.setVisible(true);
    }

    // Hiển thị giao diện Đăng nhập
    private void showLoginGUI(String accountName) {
        JFrame frame = createFrame("Login", 400, 300);
        frame.getContentPane().setBackground(new Color(240, 240, 240));
        JPanel panel = createPanel(4, 2);

        JTextField accountField = new JTextField(accountName);
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");

        Font font = new Font("Arial", Font.BOLD, 14);
        loginButton.setFont(font);

        // Xử lý nút Đăng nhập
        loginButton.addActionListener(e -> handleLogin(accountField, passwordField, frame));
        addComponentsToPanel(panel, new JLabel("Account Name:"), accountField,
                new JLabel("Password:"), passwordField, loginButton);

        frame.add(panel);
        frame.setVisible(true);
    }

    // Hiển thị giao diện Gửi email
    private void showSendEmailGUI(String accountName) {
        JFrame frame = createFrame("Send Email", 500, 400);
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(4, 1));
        JLabel accountLabel = new JLabel("Logged in as: " + accountName);
        JComboBox<String> recipientComboBox = new JComboBox<>(getRegisteredAccounts());
        JTextArea emailContentArea = new JTextArea(10, 40);
        emailContentArea.setLineWrap(true);
        emailContentArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(emailContentArea);

        JPanel buttonPanel = new JPanel();
        JButton sendButton = new JButton("Send Email");
        JButton logoutButton = new JButton("Logout");

        sendButton.addActionListener(e -> handleSendEmail(recipientComboBox, emailContentArea, frame, accountName));
        logoutButton.addActionListener(e -> logout());

        buttonPanel.add(sendButton);
        buttonPanel.add(logoutButton);

        inputPanel.add(accountLabel);
        inputPanel.add(new JLabel("Recipient:"));
        inputPanel.add(recipientComboBox);
        inputPanel.add(new JLabel("Email Content:"));

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(panel);
        frame.setVisible(true);
    }

    // Xử lý đăng ký
    private void handleRegister(JTextField accountField, JPasswordField passwordField, JFrame frame) {
        String accountName = accountField.getText();
        String password = new String(passwordField.getPassword());

        if (!accountName.isEmpty() && !password.isEmpty()) {
            try {
                File userDir = new File("MailServerAccounts/" + accountName);
                if (!userDir.exists()) {
                    userDir.mkdirs();
                    createAccountFile(userDir, accountName, password);
                    JOptionPane.showMessageDialog(frame, "Registration successful");
                    out.println("REGISTER " + accountName);
                    frame.dispose();
                    showLoginGUI(accountName);
                } else {
                    JOptionPane.showMessageDialog(frame, "Account already exists!");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Please enter account name and password!");
        }
    }

    // Tạo file tài khoản
    private void createAccountFile(File userDir, String accountName, String password) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(userDir, "account.txt")))) {
            pw.println("Account: " + accountName);
            pw.println("Password: " + password);
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(userDir, "new_mail.txt")))) {
            pw.println("Thank you for using this service. We hope that you will feel comfortable.");
        }
    }

    // Xử lý đăng nhập
    private void handleLogin(JTextField accountField, JPasswordField passwordField, JFrame frame) {
        String inputAccount = accountField.getText();
        String inputPassword = new String(passwordField.getPassword());

        if (!inputAccount.isEmpty() && !inputPassword.isEmpty()) {
            File accountFile = new File("MailServerAccounts/" + inputAccount + "/account.txt");
            if (accountFile.exists()) {
                validateLogin(accountFile, inputAccount, inputPassword, frame);
            } else {
                JOptionPane.showMessageDialog(frame, "Account does not exist!");
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Please enter account name and password!");
        }
    }

    // Xác thực đăng nhập
    private void validateLogin(File accountFile, String inputAccount, String inputPassword, JFrame frame) {
        try (BufferedReader reader = new BufferedReader(new FileReader(accountFile))) {
            String storedAccount = reader.readLine().split(": ")[1];
            String storedPassword = reader.readLine().split(": ")[1];

            if (inputAccount.equals(storedAccount) && inputPassword.equals(storedPassword)) {
                JOptionPane.showMessageDialog(frame, "Login successful!");
                out.println("LOGIN " + inputAccount);
                frame.dispose();
                checkForNewEmails(inputAccount);
                showSendEmailGUI(inputAccount);
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials!");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Kiểm tra email mới
    private void checkForNewEmails(String accountName) {
        File mailDir = new File("MailServerAccounts/" + accountName);
        
        // Danh sách để lưu các email và nội dung của chúng
        StringBuilder mailContent = new StringBuilder();
        StringBuilder emailList = new StringBuilder();
        
        // Lấy danh sách tất cả các file trong thư mục mailDir
        File[] files = mailDir.listFiles((dir, name) -> name.startsWith("mail_") && name.endsWith(".txt"));

        if (files != null && files.length > 0) {
            for (File emailFile : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(emailFile))) {
                    StringBuilder emailBody = new StringBuilder();
                    String line;

                    // Đọc nội dung email
                    while ((line = reader.readLine()) != null) {
                        emailBody.append(line).append("\n");
                    }

                    // Thêm tên email vào danh sách
                    emailList.append(emailFile.getName()).append("\n");
                    // Thêm nội dung của email vào mailContent
                    mailContent.append(emailFile.getName()).append(": ").append(emailBody.toString()).append("\n");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            // Hiển thị thông báo cho người dùng nếu có email mới
            if (emailList.length() > 0) {
                int option = JOptionPane.showConfirmDialog(null,
                    "You have new emails:\n" + emailList + "\nWould you like to view them?",
                    "New Emails",
                    JOptionPane.YES_NO_OPTION);

                // Nếu người dùng chọn "Có", hiển thị nội dung email
                if (option == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(null, "Email Content:\n" + mailContent.toString());
                }
            }
        }
    }

    // Xử lý gửi email
    private void handleSendEmail(JComboBox<String> recipientComboBox, JTextArea emailContentArea, JFrame frame, String accountName) {
        String recipient = (String) recipientComboBox.getSelectedItem();
        String emailContent = emailContentArea.getText();

        if (recipient != null && !emailContent.isEmpty()) {
            out.println("SEND_EMAIL " + accountName + " " + recipient);
            out.println(emailContent);
            out.println(); // Gửi một dòng trống để báo cho server kết thúc nội dung email
            out.println("NOTIFY " + accountName + " " + recipient); // Gửi thông báo
            JOptionPane.showMessageDialog(frame, "Email sent to " + recipient);
            emailContentArea.setText(""); // Xóa nội dung email sau khi gửi
        } else {
            JOptionPane.showMessageDialog(frame, "Please select a recipient and enter the email content.");
        }
    }

    // Đăng xuất
    private void logout() {
        out.println("LOGOUT");
        JOptionPane.showMessageDialog(null, "Logged out successfully.");
        System.exit(0);
    }

    // Tạo khung cho GUI
    private JFrame createFrame(String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        return frame;
    }

    // Tạo panel cho GUI
    private JPanel createPanel(int rows, int cols) {
        return new JPanel(new GridLayout(rows, cols));
    }

    // Thêm các thành phần vào panel
    private void addComponentsToPanel(JPanel panel, JComponent... components) {
        for (JComponent component : components) {
            panel.add(component);
        }
    }

    // Lấy danh sách các tài khoản đã đăng ký
    private String[] getRegisteredAccounts() {
        File dir = new File("MailServerAccounts");
        String[] accounts = dir.list();
        return accounts != null ? accounts : new String[0];
    }
}
