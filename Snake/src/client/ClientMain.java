package client;

import javax.swing.*;

public class ClientMain extends JFrame {

	// 클라이언트 전체에서 네트워크 연결은 한 개만 있어야 함
	// StartPanel/GamePanel 둘 다 같은 networkClient 공유해야하기 때문
    private NetworkClient networkClient;

    public ClientMain() {
        setTitle("Snake Royale");
        setSize(1200, 800);  // 시작 화면은 나중에 450x650로 바꿔도 됨
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // 네트워크 클라이언트 미리 생성 (StartPanel/GamePanel에서 같이 사용)
        networkClient = new NetworkClient();

        setContentPane(new StartPanel(this, networkClient));
        setVisible(true);
    }

    // GamePanel로 전환할 때 호출
    public void showGame(GameState initialState, String playerName) {
        setContentPane(new GamePanel(this, networkClient, initialState, playerName));
        revalidate();
        repaint();
    }

    
    // Swing은 EDT라는 스레드에서만 UI를 생성하고 수정
    // SwingUtilities.invokeLater()는 Swing에서 UI렌더링을 안전하게 처리하기 위해 전용 스레드에서 실행하도록 예약하는 역할
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientMain();
            }
        });
    }

}