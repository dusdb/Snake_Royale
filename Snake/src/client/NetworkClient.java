package client;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;

    public void addListener(GameStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameStateListener listener) {
        listeners.remove(listener);
    }

    // StartPanel에서 호출: 서버 접속 + JOIN 전송
    // 텍스트 기반 프로토콜을 사용하기 때문에 PrintWriter로 메시지를 쉽게 보내기 위해 사용
    public void connect(String host, int port, String nickname) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        running = true;

        // 간단한 텍스트 프로토콜 예시: "JOIN 닉네임"
        out.println("JOIN " + nickname);

        // 서버로부터 데이터를 받기위한 수신 스레드 시작
        //Thread receiveThread = new Thread(this::receiveLoop, "Client-Receive-Thread");
        Thread receiveThread = new Thread(() -> receiveLoop());
        receiveThread.setName("Client-Receive-Thread");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    // 방향키 입력 시 호출
    public void sendMove(String direction) {
        if (out != null) {
            // 예: "MOVE UP", "MOVE LEFT"
            out.println("MOVE " + direction);
        }
    }

    // 추후: 채팅 전송
    public void sendChat(String message) {
        if (out != null) {
            out.println("CHAT " + message);
        }
    }

    private void receiveLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                // 서버에서 오는 메시지 타입에 따라 분기
                // 예: STATE ... , CHAT ..., EXIT ...
                if (line.startsWith("STATE ")) {
                    GameState state = parseState(line.substring("STATE ".length()));
                    notifyStateUpdated(state);
                } else if (line.startsWith("CHAT ")) {
                    // 필요하면 별도 리스너 추가
                    System.out.println("[CHAT] " + line.substring(5));
                }
            }
        } catch (IOException e) {
            System.err.println("Receive loop ended: " + e.getMessage());
        } finally {
            close();
        }
    }

    private GameState parseState(String payload) {
        // TODO: 서버와 약속한 포맷대로 파싱
        // 지금은 빈 상태 리턴 (골격만)
    	// 서버에서 보낸 데이터를 파싱해서 GameState 필드에 채우기 위해 변환 코드 작성
        GameState state = new GameState();
        // 예: payload: "TICK 10|APPLE 100 200|PLAYER p1 3 100 100 ALIVE;..."
        return state;
    }

    // receiveLoop 안에서 바로 panel.repaint() 같은 걸 부르면 UI 스레드 충돌 발생 가능
    // invokeLater()로 UI 스레드로 안전하게 게임 상태를 전달하는 코드를 예약
    private void notifyStateUpdated(GameState state) {
        for (GameStateListener l : listeners) {
            // UI 스레드에서 그리도록 SwingUtilities 사용
            javax.swing.SwingUtilities.invokeLater(() -> l.onGameStateUpdated(state));
        }
    }

    public void close() {
        running = false;
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
