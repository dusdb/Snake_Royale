package client;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

public class StartPanel extends JPanel {
    private JTextField nameField;
    private JTextField hostField;
    private JTextField portField;

    // GridBagLayout은 스윙에서 가장 유연하지만 약간 까다로운 레이아웃 매니저, GridBagConstraints는 GridBagLayout에 컴포넌트를 추가할 때 사용하는 제약조건    
    // GridLayout만으로도 화면을 표처럼 나눌 수 있는데, 굳이 GridBagLayout + GridBagConstraints 조합을 쓴 이유는 자유도와 세밀한 제어 때문
    
    public StartPanel(ClientMain frame, NetworkClient networkClient) {
        setPreferredSize(new Dimension(450, 650));
        setBackground(Color.BLACK);
        setLayout(new GridBagLayout());

        // ===== Title =====
        JLabel title = new JLabel("Snake Royale");
        title.setForeground(new Color(0, 255, 128));
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        GridBagConstraints gbcTitle = new GridBagConstraints();
        gbcTitle.gridx = 0;
        gbcTitle.gridy = 0;
        gbcTitle.gridwidth = 2;
        gbcTitle.insets = new Insets(30, 10, 40, 10);
        add(title, gbcTitle);

        // ===== 사용자 이름 =====
        JLabel nameLabel = label("사용자 이름:");
        GridBagConstraints gbcNameLabel = new GridBagConstraints();
        gbcNameLabel.gridx = 0;
        gbcNameLabel.gridy = 1;
        gbcNameLabel.insets = new Insets(15, 10, 15, 10);
        add(nameLabel, gbcNameLabel);

        nameField = textField();
        GridBagConstraints gbcNameField = new GridBagConstraints();
        gbcNameField.gridx = 1;
        gbcNameField.gridy = 1;
        gbcNameField.insets = new Insets(15, 10, 15, 10);
        add(nameField, gbcNameField);

        // ===== 서버 IP =====
        JLabel hostLabel = label("서버 IP:");
        GridBagConstraints gbcHostLabel = new GridBagConstraints();
        gbcHostLabel.gridx = 0;
        gbcHostLabel.gridy = 2;
        gbcHostLabel.insets = new Insets(15, 10, 15, 10);
        add(hostLabel, gbcHostLabel);

        hostField = textField();
        hostField.setText("localhost");
        GridBagConstraints gbcHostField = new GridBagConstraints();
        gbcHostField.gridx = 1;
        gbcHostField.gridy = 2;
        gbcHostField.insets = new Insets(15, 10, 15, 10);
        add(hostField, gbcHostField);

        // ===== 포트 =====
        JLabel portLabel = label("포트:");
        GridBagConstraints gbcPortLabel = new GridBagConstraints();
        gbcPortLabel.gridx = 0;
        gbcPortLabel.gridy = 3;
        gbcPortLabel.insets = new Insets(15, 10, 15, 10);
        add(portLabel, gbcPortLabel);

        portField = textField();
        portField.setText("5000");
        GridBagConstraints gbcPortField = new GridBagConstraints();
        gbcPortField.gridx = 1;
        gbcPortField.gridy = 3;
        gbcPortField.insets = new Insets(15, 10, 15, 10);
        add(portField, gbcPortField);

        // ===== START 버튼 =====
        JButton startButton = new JButton("START");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        startButton.setBackground(new Color(0, 255, 128));
        startButton.setForeground(Color.BLACK);
        startButton.setFocusPainted(false);
        startButton.setBorder(new RoundedBorder(new Color(0, 255, 128), 2, 20));
        startButton.setPreferredSize(new Dimension(150, 45));

        // 버튼 클릭 동작
        startButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "이름을 입력하세요!");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "포트 번호가 올바르지 않습니다.");
                return;
            }

            // 입력한 정보로 네트워크 연결 시도 및 GamePanel로 넘어감
            try {
                networkClient.connect(host, port, name);
                // 연결 성공 → 빈 GameState로 시작 (곧 서버에서 첫 STATE가 옴)
                frame.showGame(new GameState(), name);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "서버에 연결할 수 없습니다: " + ex.getMessage());
            }
        });

        GridBagConstraints gbcStartButton = new GridBagConstraints();
        gbcStartButton.gridx = 0;
        gbcStartButton.gridy = 4;
        gbcStartButton.gridwidth = 2;
        gbcStartButton.insets = new Insets(40, 10, 10, 10); // 입력란 아래 적당한 간격
        add(startButton, gbcStartButton);
    }

    // ===== 텍스트 라벨 생성 =====
    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return l;
    }

    // ===== 입력 필드 생성 =====
    private JTextField textField() {
        JTextField tf = new JTextField(12);
        tf.setForeground(Color.WHITE);
        tf.setBackground(Color.BLACK);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(new RoundedBorder(new Color(0, 255, 128), 2, 12));
        return tf;
    }

    // ===== 둥근 테두리 =====
    static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        public RoundedBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x + thickness / 2, y + thickness / 2,
                    width - thickness, height - thickness, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }
    }
}
