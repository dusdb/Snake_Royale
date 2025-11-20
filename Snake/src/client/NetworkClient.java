package client;

import java.awt.Point;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

public class NetworkClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // CopyOnWriteArrayList는 여러 스레드에서 동시에 add/remove해도 안전한 리스트
    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;

    
    // 네트워크 패킷을 처리해서 UI에 반영할 때, GamePanel처럼 GameState를 수신해야 하는 객체를 등록
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

    
    private void receiveLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {

            	// 서버에서 받은 모든 텍스트를 콘솔에 출력하는 디버깅용 코드
                System.out.println("RECV >>> " + line);  

                // STATE 데이터 처리 흐름
                // 1. 서버에서 STATE가 전송됨
                // 2. STATE 문자열만 추출
                // 3. parseState(payload) 호출 -> payload에서 뱀, 사과, 점수 정보를 각각 분해해서 GameState에 넣음
                // 4. notifyStateUpdated(state) - UI 스레드에서 안전하게 실행되도록 invokeLater로 전달
                // 5. GamePanel.onGameStateUpdated(state) - 새 상태로 UI 갱신
                if (line.startsWith("STATE") || line.startsWith("STATE_UPDATE")) {

                    String payload = line.substring(line.indexOf(" ") + 1).trim();
                    System.out.println("⚠ RAW STATE = " + payload);

                    GameState state = parseState(payload);
                    notifyStateUpdated(state);
                }
                // 서버 메시지 처리 흐름
                // 1. 서버가 CHAT 메시지를 보냄
                // 2. 클라이언트에서 수신
                // 3. GameStateListener에 있는 onChatMessage 메서드로 모든 Listener에게 전달
                // 4. GamePanel에서 메시지 표시
                else if (line.startsWith("CHAT")) {
                    notifyChatMessage(line.substring(5));
                }
                
                // 서버 게임 종료 처리 흐름
                // 1. 서버가 GAMEOVER 메시지를 보냄
                // 2. 클라이언트에서 수신
                // 3. GameStateListener에 있는 onGameOver 메서드로 모든 Listener에게 전달
                // 4. GamePanel에서 GameOverPAnel로 변경
                else if (line.startsWith("GAMEOVER")) {
                    notifyGameOver();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    private void notifyChatMessage(String msg) {
        for (GameStateListener l : listeners) {
            SwingUtilities.invokeLater(() -> l.onChatMessage(msg));
        }
    }

    private void notifyGameOver() {
        for (GameStateListener l : listeners) {
            SwingUtilities.invokeLater(l::onGameOver);
        }
    }


    private GameState parseState(String payload) {

        GameState gs = new GameState();
        if (payload == null || payload.isEmpty()) return gs;

        String[] parts = payload.split("\\|");

        /** -------------- [0]  뱀 정보 ------------------ */
        if (parts.length > 0) {
            String[] players = parts[0].split(";");

            for (String p : players) {

                if (!p.contains(":")) continue;

                String name = p.substring(0, p.indexOf(":"));
                String bodyStr = p.substring(p.indexOf(":") + 1, p.indexOf("("));
                boolean alive = p.contains("(A)");

                List<Point> body = new ArrayList<>();
                String[] pts = bodyStr.split(",");

                for (int i = 0; i < pts.length - 1; i += 2) {
                    body.add(new Point(
                            Integer.parseInt(pts[i]),
                            Integer.parseInt(pts[i + 1])
                    ));
                }

                gs.snakeBodies.put(name, body);
                gs.snakeAlive.put(name, alive);
            }
        }

        /** -------------- [1]  사과 정보 ------------------ */
        if (parts.length > 1 && parts[1].startsWith("A:")) {
            String[] xy = parts[1].substring(2).split(",");
            gs.appleX = Integer.parseInt(xy[0]);
            gs.appleY = Integer.parseInt(xy[1]);
        }

        /** -------------- [2]  점수 정보 ------------------ */
        if (parts.length > 2 && parts[2].startsWith("S:")) {
            String[] scoreData = parts[2].substring(2).split(",");
            for (String s : scoreData) {
                String[] kv = s.split("=");
                if (kv.length == 2) {
                    gs.scores.put(kv[0], Integer.parseInt(kv[1]));
                }
            }
        }
        
        System.out.println("⚠ RAW STATE = " + payload);


        return gs;
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
