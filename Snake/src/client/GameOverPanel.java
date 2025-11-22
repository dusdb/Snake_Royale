package client;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GameOverPanel extends JPanel {

    public GameOverPanel(ClientMain frame, GameState gameState) {

        setBackground(Color.BLACK);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Game Over");
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setForeground(Color.RED);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        add(Box.createVerticalStrut(40));
        add(title);

        JLabel rankLabel = new JLabel("최종 순위");
        rankLabel.setAlignmentX(CENTER_ALIGNMENT);
        rankLabel.setForeground(new Color(0, 255, 0));
        rankLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(Box.createVerticalStrut(20));
        add(rankLabel);

        // 게임 상태를 생성자에서 전달받아 내림차순으로 정렬 후 포맷에 맞게 화면에 출력 
        java.util.List<String> ranking = gameState.scores.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .map(e -> e.getKey())
                .toList();

        for (int i = 0; i < ranking.size(); i++) {
            JLabel player = new JLabel((i + 1) + ". " + ranking.get(i));
            player.setAlignmentX(CENTER_ALIGNMENT);
            player.setForeground(Color.WHITE);
            player.setFont(new Font("SansSerif", Font.PLAIN, 18));
            add(Box.createVerticalStrut(10));
            add(player);
        }

        JButton exitBtn = new JButton("나가기");
        exitBtn.setAlignmentX(CENTER_ALIGNMENT);
        exitBtn.setBackground(new Color(255, 80, 80));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        exitBtn.addActionListener(e -> {
            frame.setContentPane(new StartPanel(frame, new NetworkClient()));
            frame.revalidate();
        });

        add(Box.createVerticalStrut(30));
        add(exitBtn);
    }
}
