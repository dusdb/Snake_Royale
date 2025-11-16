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

        // 게임 영역
        GameCanvas canvas = new GameCanvas();
        add(canvas, BorderLayout.CENTER);

        // 사이드 영역
        sidePanel = new SidePanel(frame, myName);
        add(sidePanel, BorderLayout.EAST);

        // 서버 상태 업데이트 등록
        networkClient.addListener(this);

        // 🔥 키 입력을 canvas가 직접 받도록 설정
        canvas.setFocusable(true);

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

        // 🔥 매우 중요!!! → 화면이 표시된 뒤 포커스를 canvas에 강제로 줌
        SwingUtilities.invokeLater(() -> {
            canvas.requestFocusInWindow();
            System.out.println("⚡ Canvas Focus Activated");
        });
    }

    @Override
    public void onGameStateUpdated(GameState state) {
        this.gameState = state;
        repaint();

        if (!state.scores.isEmpty()) {
            List<String> ranking = state.scores.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .map(e -> e.getKey() + " : " + e.getValue())
                    .toList();

            sidePanel.updateRanking(ranking);
        }
    }

    @Override
    public void onChatMessage(String msg) {
        sidePanel.appendSystemMessage(msg);
    }


    // ====================== 🎮 게임 화면 ====================== //
    class GameCanvas extends JPanel {
        GameCanvas() {
            setPreferredSize(new Dimension(960, 760));
            setBackground(Color.BLACK);
        }

        // 🔥 화면 재배치 후에도 포커스를 계속 유지
        @Override
        public void addNotify() {
            super.addNotify();
            requestFocusInWindow();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (gameState.snakeBodies.isEmpty()) return;

            // 🍎 사과
            g.setColor(Color.RED);
            g.fillOval(gameState.appleX, gameState.appleY, 20, 20);

            // 🐍 모든 뱀
            for (String name : gameState.snakeBodies.keySet()) {
                java.util.List<Point> body = gameState.snakeBodies.get(name);
                boolean alive = gameState.snakeAlive.get(name);

                g.setColor(alive ? Color.GREEN : Color.GRAY);

                for (Point p : body) {
                    g.fillRect(p.x, p.y, 20, 20);
                }
            }
        }
    }


    // ====================== 📊 오른쪽 패널 ====================== //
    static class SidePanel extends JPanel {

        private final DefaultListModel<String> rankModel;
        private final JTextArea systemLog;

        public SidePanel(ClientMain frame, String playerName) {

            setPreferredSize(new Dimension(220, 600));
            setBackground(Color.BLACK);
            setLayout(new GridBagLayout());
            setBorder(new LineBorder(new Color(0, 255, 128), 2));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 10, 10);

            JLabel rankLabel = new JLabel("순위", SwingConstants.CENTER);
            rankLabel.setForeground(Color.WHITE);
            rankLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

            gbc.gridy = 0;
            add(rankLabel, gbc);

            rankModel = new DefaultListModel<>();
            JList<String> rankList = new JList<>(rankModel);
            rankList.setBackground(Color.BLACK);
            rankList.setForeground(Color.WHITE);

            JScrollPane rankScroll = new JScrollPane(rankList);
            rankScroll.setBorder(new LineBorder(new Color(0, 255, 128), 1));

            gbc.gridy = 1;
            gbc.weighty = 0.25;
            gbc.fill = GridBagConstraints.BOTH;
            add(rankScroll, gbc);

            systemLog = new JTextArea(playerName + "님 환영합니다.\n");
            systemLog.setEditable(false);
            systemLog.setBackground(Color.BLACK);
            systemLog.setForeground(Color.WHITE);

            JScrollPane logScroll = new JScrollPane(systemLog);
            logScroll.setBorder(new LineBorder(new Color(0, 255, 128), 1));

            gbc.gridy = 2;
            gbc.weighty = 0.65;
            gbc.fill = GridBagConstraints.BOTH;
            add(logScroll, gbc);

            JButton exitButton = new JButton("나가기");
            exitButton.setBackground(new Color(255, 70, 70));
            exitButton.setForeground(Color.WHITE);

            gbc.gridy = 3;
            gbc.weighty = 0;
            add(exitButton, gbc);

            exitButton.addActionListener(e -> {
                frame.setContentPane(new StartPanel(frame, new NetworkClient()));
                frame.revalidate();
            });
        }

        public void updateRanking(List<String> names) {
            rankModel.clear();
            for (int i = 0; i < names.size(); i++) {
                rankModel.addElement((i + 1) + ". " + names.get(i));
            }
        }

        public void appendSystemMessage(String msg) {
            SwingUtilities.invokeLater(() -> {
                systemLog.append(msg + "\n");
                systemLog.setCaretPosition(systemLog.getDocument().getLength());
            });
        }
    }
}
