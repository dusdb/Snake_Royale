package client;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class GamePanel extends JPanel implements GameStateListener {

    private final NetworkClient networkClient;
    private GameState gameState;
    private final String myName;
    private final SidePanel sidePanel;

    public GamePanel(ClientMain frame, NetworkClient networkClient,
                     GameState initialState, String myName) {
        this.networkClient = networkClient;
        this.gameState = initialState;
        this.myName = myName;

        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        // 🎮 게임 영역
        GameCanvas canvas = new GameCanvas();
        add(canvas, BorderLayout.CENTER);

        // 📋 사이드 영역
        sidePanel = new SidePanel(frame, myName);
        add(sidePanel, BorderLayout.EAST);

        // 🧠 서버 상태 업데이트 등록
        networkClient.addListener(this);

        // 방향키 입력을 서버로 전송
        canvas.setFocusable(true);
        canvas.requestFocusInWindow();
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> networkClient.sendMove("UP");
                    case KeyEvent.VK_DOWN -> networkClient.sendMove("DOWN");
                    case KeyEvent.VK_LEFT -> networkClient.sendMove("LEFT");
                    case KeyEvent.VK_RIGHT -> networkClient.sendMove("RIGHT");
                }
            }
        });
    }

    @Override
    public void onGameStateUpdated(GameState state) {
        this.gameState = state;
        repaint();

        // 서버에서 받은 순위 리스트나 메시지 반영 예시
        if (state.rankList != null) {
            sidePanel.updateRanking(state.rankList);
        }
        if (state.systemMessages != null && !state.systemMessages.isEmpty()) {
            for (String msg : state.systemMessages) {
                sidePanel.appendSystemMessage(msg);
            }
            state.systemMessages.clear(); // 중복 방지
        }
    }

    // 🟩 실제 게임 화면
    class GameCanvas extends JPanel {
        GameCanvas() {
            setPreferredSize(new Dimension(600, 600));
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (gameState == null) return;

            // 🍎 사과
            g.setColor(Color.RED);
            g.fillOval(gameState.appleX, gameState.appleY, 20, 20);

            // 🐍 뱀
            if (gameState.snakes != null) {
                for (SnakeInfo s : gameState.snakes) {
                    g.setColor(s.color);
                    for (Point p : s.segments) {
                        g.fillRect(p.x, p.y, 20, 20);
                    }
                }
            }

            // 💀 게임 오버
            if (gameState.gameOver) {
                g.setColor(Color.RED);
                g.setFont(new Font("SansSerif", Font.BOLD, 40));
                String msg = "Game Over";
                int w = g.getFontMetrics().stringWidth(msg);
                g.drawString(msg, (getWidth() - w) / 2, getHeight() / 2);
            }
        }
    }

    // 📋 오른쪽 패널 (순위 + 나가기 + 시스템 메시지 로그)
    static class SidePanel extends JPanel {

        private final DefaultListModel<String> rankModel;
        private final JTextArea systemLog;

        public SidePanel(ClientMain frame, String playerName) {
            setPreferredSize(new Dimension(220, 600));
            setBackground(Color.BLACK);
            setLayout(new GridBagLayout());
            setBorder(new LineBorder(new Color(0, 255, 128), 1));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 5, 10, 5);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;

            // 🏆 순위표
            JLabel rankLabel = new JLabel("순위", SwingConstants.CENTER);
            rankLabel.setForeground(Color.WHITE);
            rankLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            gbc.gridy = 0;
            gbc.weighty = 0.05;
            add(rankLabel, gbc);

            rankModel = new DefaultListModel<>();
            JList<String> rankList = new JList<>(rankModel);
            rankList.setBackground(Color.BLACK);
            rankList.setForeground(Color.WHITE);
            rankList.setBorder(new LineBorder(new Color(0, 255, 128), 1));
            gbc.gridy = 1;
            gbc.weighty = 0.15;
            add(new JScrollPane(rankList), gbc);

            // 🚪 나가기 버튼
            JButton exitButton = new JButton("나가기");
            exitButton.setBackground(new Color(255, 70, 70));
            exitButton.setForeground(Color.WHITE);
            exitButton.setFont(new Font("SansSerif", Font.BOLD, 16));
            exitButton.setFocusPainted(false);
            exitButton.addActionListener(e -> {
                frame.setContentPane(new StartPanel(frame, new NetworkClient()));
                frame.revalidate();
            });
            gbc.gridy = 2;
            gbc.weighty = 0.05;
            add(exitButton, gbc);

            // 💬 서버 시스템 로그 (읽기 전용)
            systemLog = new JTextArea(playerName + "님 환영합니다.\n");
            systemLog.setEditable(false);
            systemLog.setBackground(Color.BLACK);
            systemLog.setForeground(Color.WHITE);
            systemLog.setFont(new Font("SansSerif", Font.PLAIN, 13));
            systemLog.setBorder(new LineBorder(new Color(0, 255, 128), 1));
            gbc.gridy = 3;
            gbc.weighty = 0.75;
            add(new JScrollPane(systemLog), gbc);
        }

        // 🔄 순위 업데이트
        public void updateRanking(List<String> names) {
            rankModel.clear();
            for (int i = 0; i < names.size(); i++) {
                rankModel.addElement((i + 1) + ". " + names.get(i));
            }
        }

        // 📨 서버에서 온 시스템 메시지 추가
        public void appendSystemMessage(String msg) {
            SwingUtilities.invokeLater(() -> {
                systemLog.append(msg + "\n");
                systemLog.setCaretPosition(systemLog.getDocument().getLength());
            });
        }
    }
}
