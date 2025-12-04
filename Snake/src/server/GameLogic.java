package server;

import java.awt.Point; // 지렁이와 사과의 좌표(x, y)를 관리하기 위해 사용
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Runnable로 별도의 게임 루프 스레드로 동작
public class GameLogic implements Runnable {
    
    // 게임 보드 크기 (GamePanel -> 960x760에 20px 단위로 그림)
    public static final int BOARD_WIDTH = 48; // 960px / 20px
    public static final int BOARD_HEIGHT = 38;  // 760px / 20px
    private final int TICK_RATE_MS = 120;     // 0.15초마다 게임 상태 갱신 (지렁이 속도)

    private ServerMain server; // broadcast를 위한 서버 참조
    private Random rand = new Random();

    // 여러 ClientHandler가 동시에 지렁이의 방향을 바꿀 수 있기 때문에 스레드에 안전한 Map을 사용
    private Map<String, SnakeInfo> snakes = new ConcurrentHashMap<>();
    private Map<String, ClientHandler> playerHandlers = new ConcurrentHashMap<>();
    
    private Point apple;

    public GameLogic(ServerMain server) {
        this.server = server;
        spawnApple(); // 서버 시작 시 최초 사과 생성
    }

    // 게임 루프 스레드 
    // ServerMain에서 new Thread(this).start())가 실행하는 메인 메소드
    @Override
    public void run() {
        while (true) {
            try {
                // (선순위 규칙 반영) 모든 게임 로직(이동, 충돌, 사과) 업데이트
                updateGame();
                              
                // 위치 계산 후 전송을 위해 갱신된 게임 상태를 문자열로 변환
                String stateString = getGameStateString();
                server.broadcast(stateString);
                
                // 다음 틱(Tick)까지 대기 (지렁이 속도 조절)
                Thread.sleep(TICK_RATE_MS);
                
            } catch (InterruptedException e) {
                System.out.println("게임 루프가 중지되었습니다.");
                break; 
            }
        }
    }
    
    // 플레이어의 방향 변경 요청 처리 
    public void setDirection(String clientName, String direction) {
        SnakeInfo snake = snakes.get(clientName);
        if (snake != null && snake.isAlive) {
            snake.setDirection(direction);
        }
    }
    
    // 사과 생성 로직 (랜덤)
    public synchronized void spawnApple() {
        while (true) {
            int x = rand.nextInt(BOARD_WIDTH);
            int y = rand.nextInt(BOARD_HEIGHT);
            
            Point potentialApple = new Point(x, y); // Point 객체로 사과 위치 저장
            boolean isOverlapping = false;
            
            // 지렁이의 몸통과 겹치지 않는 위치인지 검사
            for (SnakeInfo snake : snakes.values()) {
                for (Point bodyPart : snake.body) {
                    if (bodyPart.equals(potentialApple)) {
                        isOverlapping = true;
                        break;
                    }
                }
                if (isOverlapping) break;
            }
            if (!isOverlapping) {
                apple = potentialApple;
                break;
            }
        }
    }

    // 새 플레이어 추가
    public synchronized void addPlayer(String clientName, ClientHandler handler) {
        SnakeInfo newSnake = new SnakeInfo(clientName, 10, 10);
        snakes.put(clientName, newSnake);
        playerHandlers.put(clientName, handler);  
    }

    // GameLogic에서 플레이어 제거
    public synchronized void removePlayer(String clientName) {
        snakes.remove(clientName);
        playerHandlers.remove(clientName);
    }
    
    // 게임의 한 프레임 업데이트 로직
    // 이동 -> 사과 섭취 -> 충돌 판정 -> 사망 처리
    private synchronized void updateGame() {
        if (snakes.isEmpty()) return; // 플레이어 없으면 아무것도 안함
        
        List<String> deadSnakes = new ArrayList<>();

        // 1. 모든 살아있는 지렁이 이동
        for (SnakeInfo snake : snakes.values()) {
            if (snake.isAlive) {
                snake.move();
            }
        }

        // 2. 사과 섭취 검사
        for (SnakeInfo snake : snakes.values()) {
            if (!snake.isAlive) continue;
            if (snake.getHead().equals(apple)) {
                snake.eat();
                spawnApple();
            }
        }

        // 3. 충돌 판정
        for (SnakeInfo snake : snakes.values()) {
            if (!snake.isAlive) continue;

            Point head = snake.getHead();

            // (3-1) 다른 지렁이들과의 충돌 검사
            for (SnakeInfo other : snakes.values()) {
                if (!other.isAlive) continue;
                if (snake == other) continue;

                // (A) 머리끼리 충돌
                if (snake.getHead().equals(other.getHead())) {
                    snake.die();
                    other.die();
                    deadSnakes.add(snake.name);
                    deadSnakes.add(other.name);
                    break;
                }

                // (B) 내 머리가 다른 지렁이 몸통과 충돌
                if (other.checkBodyCollision(head)) {
                    snake.die();
                    deadSnakes.add(snake.name);
                    other.addKillScore();
                    other.grow(5);
                    break;
                }
            }

            if (!snake.isAlive) continue;

            // (3-2) 벽 충돌
            if (head.x < 0 || head.x >= BOARD_WIDTH || head.y < 0 || head.y >= BOARD_HEIGHT) {
                snake.die();
                deadSnakes.add(snake.name);
                continue;
            }

            // (3-3) 자기 몸과 충돌
            if (snake.checkSelfCollision()) {
                snake.die();
                deadSnakes.add(snake.name);
                continue;
            }
        }
        
        // 4. 사망한 플레이어 처리
        for (String deadName : deadSnakes) {
            ClientHandler handler = playerHandlers.get(deadName);
            if (handler != null) {
                handler.sendMessage("GAMEOVER");  // GAMEOVER 알림 전송
                handler.disconnect(); // 사망한 플레이어의 ClientHandler 소켓 즉시 종료
                removePlayer(deadName); // 목록에서 제거
            }
        }   
    }
    
    // 현재 게임 상태 문자열 생성
    private synchronized String getGameStateString() {
        StringBuilder sb = new StringBuilder("STATE ");
        for (SnakeInfo snake : snakes.values()) {
            sb.append(snake.toString());
        }
        if (apple != null) { // 사과가 없는 예외적 상황에서 서버가 죽지 않기 위함
        	sb.append("|A:").append(apple.x * 20).append(",").append(apple.y * 20);
        }
  
        sb.append("|S:");
        for (SnakeInfo snake : snakes.values()) {
            sb.append(snake.name).append("=").append(snake.score).append(",");
        }
        if (!snakes.isEmpty()) {
        	sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
}
