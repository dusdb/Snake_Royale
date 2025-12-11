package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

public class GamePanel extends JPanel implements GameStateListener {

    private final NetworkClient networkClient;
    private GameState gameState;
    // 종 게임 상태를 보존하기 위한 백업 데이터
    // 어떤 상황(먼저 죽거나 혼자 플레이할 때 등)에서도 게임 종료 화면이 제대로 표시
    private GameState lastState; 
    private final String myName;
    private final SidePanel sidePanel;
    
    // 게임 전체 동안의 점수를 누적해서 들고 있을 맵
    private final java.util.Map<String, Integer> allScores = new java.util.HashMap<>();

    public GamePanel(ClientMain frame, NetworkClient networkClient,
                     GameState initialState, String myName) {
        this.networkClient = networkClient;
        this.gameState = initialState;
        this.lastState = initialState;
        this.myName = myName;

        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        // 게임 영역
        GameCanvas canvas = new GameCanvas();
        add(canvas, BorderLayout.CENTER);

        // 사이드 영역
        sidePanel = new SidePanel(frame, myName, networkClient);
        add(sidePanel, BorderLayout.EAST);

        // 서버 상태 업데이트 등록
        networkClient.addListener(this);

        // 키 입력을 canvas가 직접 받도록 설정
        // 내부에 있는 여러 요소들이 있기 때문에 포커스를 줘서 키 입력을 포커스를 가진 컴포넌트가 받을 수 있게 함
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

        // 화면이 표시된 뒤 포커스를 canvas에 강제로 줌
        // Swing에서는 생성자 안에서 requestFocusInWindow 호출하면 대부분 무시되기 때문에 
        // invokeLater를 사용해서 화면 렌더링 이후에 포커스를 줌
        SwingUtilities.invokeLater(() -> {
            canvas.requestFocusInWindow();
        });
        
        // KeyListener 먼저 설정되고, 실제로 키 입력을 받을 수 있게 되는 건 화면이 뜬 뒤 
        // invokeLater에서 requestFocusInWindow가 실행된 순간
    }

    
    // 서버에서 받은 점수를 내림차순으로 정렬한 뒤 닉네임:점수 형식으로 변환하여 오른쪽 순위판에 반영
    @Override
    public void onGameStateUpdated(GameState state) {
        this.gameState = state;
        this.lastState = state;
        repaint();

        
        // 새로 받은 점수를 allScores에 누적 (없던 플레이어는 추가, 있던 플레이어는 갱신)
        state.scores.forEach((name, score) -> {
            allScores.put(name, score);
        });

        // 순위판을 state.scores 대신 누적 맵 기준으로 갱신
        if (!allScores.isEmpty()) {
            List<String> ranking = allScores.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .map(e -> e.getKey() + " : " + e.getValue())
                    .toList();

            sidePanel.updateRanking(ranking);
        }
        /*
        if (!state.scores.isEmpty()) {
            List<String> ranking = state.scores.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .map(e -> e.getKey() + " : " + e.getValue())
                    .toList();

            sidePanel.updateRanking(ranking);
        }*/
    }

    
    // 서버에서 CHAT 메시지를 받으면 오른쪽 로그창에 출력하기 위한 전달자 역할
    @Override
    public void onChatMessage(String msg) {
        sidePanel.appendSystemMessage(msg);
    }
    
    // 서버에서 GAMEOVER 메시지를 받으면 오른쪽 로그창에 출력하기 위한 전달자 역할
    @Override
    public void onGameOver(GameState finalState) {
    	// 이 GamePanel을 리스너에서 제거
    	networkClient.removeListener(this);
    	
    	
    	finalState.scores = new java.util.HashMap<>(allScores);
    	
        SwingUtilities.invokeLater(() -> {
            ClientMain frame = (ClientMain) SwingUtilities.getWindowAncestor(this);
            
            frame.setContentPane(new GameOverPanel(frame, finalState, networkClient));
            frame.revalidate();
        });
    }

    @Override
    public void removeNotify() {
        networkClient.removeListener(this);
        super.removeNotify();
    }


    class GameCanvas extends JPanel {
        GameCanvas() {
            setPreferredSize(new Dimension(960, 760));
            setBackground(Color.BLACK);
        }

        // 화면 재배치 후에도 포커스를 계속 유지
        @Override
        public void addNotify() {
            super.addNotify();
            requestFocusInWindow();
        }

        // 현재 게임 상황(사과, 뱀(플레이어), 생존 여부)를 화면에 시각적으로 그림
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (gameState.snakeBodies.isEmpty()) return;

            // 사과
            g.setColor(Color.RED);
            g.fillOval(gameState.appleX, gameState.appleY, 20, 20);

            // 모든 뱀을 가져온 뒤 생존 여부를 확인 후 화면에 표시
            for (String name : gameState.snakeBodies.keySet()) {
                boolean alive = gameState.snakeAlive.get(name);
                if (!alive) continue;

                java.util.List<Point> body = gameState.snakeBodies.get(name);
                Color snakeColor = gameState.snakeColors.getOrDefault(name, Color.GREEN);
                
                g.setColor(alive ? snakeColor : Color.GRAY);

                for (Point p : body) {
                    g.fillRect(p.x, p.y, 20, 20);
                }
            }
        }
    }


    static class SidePanel extends JPanel {

        private final DefaultListModel<String> rankModel;
        private final JTextArea systemLog;
        private final NetworkClient networkClient;

        // NetworkClient를 SidePanel에도 전달하여 나가기할 때 기존 networkClient.close로 정상 종료
        // StartPanel로 돌아갈 때 새로운 NetworkClient를 정상적으로 전달
        public SidePanel(ClientMain frame, String playerName, NetworkClient networkClient) {
            this.networkClient = networkClient;

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
                if (networkClient != null) {
                    networkClient.close();  // 기존 연결 안전 종료
                }
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

        
        // JTextArea에 메시지를 추가하는 기능
        public void appendSystemMessage(String msg) {
            SwingUtilities.invokeLater(() -> {
                systemLog.append(msg + "\n");
                systemLog.setCaretPosition(systemLog.getDocument().getLength());
            });
        }
    }
}