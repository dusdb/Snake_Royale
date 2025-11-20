package server;

import java.awt.Point; // 뱀과 사과의 좌표(x, y)를 관리하기 위해 사용
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// 게임의 모든 로직 담당 클래스
// Runnable로 별도의 게임 루프 스레드로 동작
public class GameLogic implements Runnable {
    
    // 게임 보드 크기 (GamePanel -> 600x600에 20px 단위로 그림)
    public static final int BOARD_WIDTH = 48; // 600px / 20px
    public static final int BOARD_HEIGHT = 38;  // 600px / 20px
    
    // 0.15초마다 게임 상태 갱신 (지렁이 속도)
    private final int TICK_RATE_MS = 120; 

    private ServerMain server; // broadcast를 위한 서버 참조
    private Random rand = new Random();

    // 여러 ClientHandler가 동시에 뱀의 방향을 바꿀 수 있기 때문에 스레드에 안전한 Map 사용
    private Map<String, SnakeInfo> snakes = new ConcurrentHashMap<>();
    private Map<String, ClientHandler> playerHandlers = new ConcurrentHashMap<>();
    private Point apple;

    // 생성자: 서버의 참조를 받아 초기화
    public GameLogic(ServerMain server) {
        this.server = server;
        spawnApple(); // 사과 생성
    }

    // 게임 루프 스레드 
    // ServerMain에서 new Thread(this).start())가 실행하는 메인 메소드
    @Override
    public void run() {
        while (true) {
            try {
                // (선순위 규칙 반영) 모든 게임 로직(이동, 충돌, 사과) 업데이트
                updateGame();
                              
                // (위치 계산 후 전송) 갱신된 게임 상태를 문자열로 변환
                String stateString = getGameStateString();
                
                // 모든 클라이언트에게 현재 게임 상태 전파
                server.broadcast(stateString);
                
                // 다음 틱(Tick)까지 대기 (지렁이 속도 조절)
                Thread.sleep(TICK_RATE_MS);
                
            } catch (InterruptedException e) {
                System.out.println("게임 루프가 중지되었습니다.");
                break; // 스레드 종료
            }
        }
    }
    
    // 사과 생성 (랜덤)
    public synchronized void spawnApple() {
        // 뱀 몸통과 겹치지 않는 위치에 생성하는 로직 추가하면 좋을듯?
    	// BOARD_WIDTH, BOARD_HEIGHT 안에서 랜덤하게 사과 좌표를 생성
        int x = rand.nextInt(BOARD_WIDTH);
        int y = rand.nextInt(BOARD_HEIGHT);
        apple = new Point(x, y); // Point 객체로 사과 위치 저장
        System.out.println("새 사과 생성: (" + x + ", " + y + ")");
    }

    // 방향키 받기
    // ClientHandler가 이 메소드를 호출하여 뱀의 다음 방향을 설정
    public void setDirection(String clientName, String direction) {
    	SnakeInfo snake = snakes.get(clientName);
        if (snake != null && snake.isAlive) {
            snake.setDirection(direction);
        }
    }

    // ClientHandler가 호출하여 GameLogic에 새 플레이어를 추가
    public synchronized void addPlayer(String clientName, ClientHandler handler) {
        // 새 플레이어 뱀 생성 (시작 위치: 10, 10)
    		SnakeInfo newSnake = new SnakeInfo(clientName, 10, 10);
        snakes.put(clientName, newSnake);
      
        playerHandlers.put(clientName, handler);  
    }

    // ClientHandler가 호출하여 GameLogic에서 플레이어를 제거
    public synchronized void removePlayer(String clientName) {
        snakes.remove(clientName);
        playerHandlers.remove(clientName);
    }
    
    // 우선순위 규칙 반영
    // 1.이동 2.사과섭취 3.충돌판정
    // run 메소드가 주기적으로 호출하는 메인 업데이트 메소드
    private synchronized void updateGame() {
        if (snakes.isEmpty()) return; // 플레이어가 없으면 아무것도 안함
        
        // 이번 턴에 죽은 뱀들을 기록할 리스트 생성
        List<String> deadSnakes = new ArrayList<>();
        
        // 1. 모든 뱀 동시 이동 (위치 계산)
        for (SnakeInfo snake : snakes.values()) {
            if (snake.isAlive) {
                snake.move(); // 뱀이 설정된 방향으로 한 칸 스스로 이동 -> 한 틱에서 반영
            }
        }
        
        // 2. 사과 섭취 및 점수 시스템 (사과 먹었는지 판정)
        for (SnakeInfo snake : snakes.values()) {
            if (!snake.isAlive) continue;
            
            // 이동이 끝난 후, 머리 위치가 사과와 겹치는지 확인
            // 뱀 머리 위치가 apple 좌표와 같으면 사과를 먹은 것으로 간주
            if (snake.getHead().equals(apple)) {
                snake.eat(); // 몸 길이, 점수 증가 (+1 씩)
                spawnApple(); // 사과는 사라지고 랜덤 위치에 사과 재생성
            }
        }

        // 3. 충돌 판정 (플레이어 죽음 처리)
        for (SnakeInfo snake : snakes.values()) {
            if (!snake.isAlive) continue; // 이미 죽은 뱀은 검사 안함
  
            Point head = snake.getHead();

            // 3-1. 다른 뱀과 충돌 (A, B)
            for (SnakeInfo otherSnake : snakes.values()) {
                if (!otherSnake.isAlive) continue; // 죽은 뱀과는 충돌 안함
                if (snake == otherSnake) continue; // 자기 자신과는 비교 안함

                // (A) 머리 vs 머리 충돌
                if (snake.getHead().equals(otherSnake.getHead())) {
                    // 머리끼리 충돌 시 둘 다 사망
                    snake.die();
                    otherSnake.die();
                    // 죽은 목록에 추가
                    deadSnakes.add(snake.name);
                    deadSnakes.add(otherSnake.name);
                    
                    System.out.println(snake.name + "와 " + otherSnake.name + "가 머리 충돌!");
                    break; // 한 번만 처리
                }

                // (B) 내 머리 vs 다른 뱀 몸통 충돌
                if (otherSnake.checkBodyCollision(head)) {
                    // 내 머리가 다른 뱀의 몸에 충돌
                    snake.die();
                    deadSnakes.add(snake.name); 
                    
                    otherSnake.addKillScore(); // 점수 획득 (+5)
                    otherSnake.grow(5); // 몸 길이 증가 (+5)
                    
                    System.out.println(snake.name + "가 " + otherSnake.name + "의 몸에 충돌!");
                    break; // 다른 뱀은 더 볼 필요 없음
                }
            }

            if (!snake.isAlive) continue; // 다른 뱀 충돌로 죽었으면 이후 검사 안함

            // 3-2. 벽 충돌
            if (head.x < 0 || head.x >= BOARD_WIDTH || head.y < 0 || head.y >= BOARD_HEIGHT) {
                snake.die(); // 뱀 죽음 처리
                deadSnakes.add(snake.name);
                System.out.println(snake.name + "가 벽에 충돌했습니다.");
                continue;
            }

            // 3-3. 자기 자신과 충돌
            if (snake.checkSelfCollision()) {
                snake.die();
                deadSnakes.add(snake.name);
                System.out.println(snake.name + "가 자기 몸에 충돌했습니다.");
                continue;
            }
        }
        
        // 4. 사망자 처리 및 개별 통보
        for (String deadSnakeName : deadSnakes) {
            
            // [변경] ServerMain을 거치지 않고, 저장해둔 핸들러를 꺼내서 바로 전송!
            ClientHandler handler = playerHandlers.get(deadSnakeName);
            if (handler != null) {
                handler.sendMessage("GAMEOVER"); // 💥 너한테만 바로 전송!
                System.out.println(deadSnakeName + "에게 GAMEOVER 전송 완료");
            }
            
            removePlayer(deadSnakeName); // 리스트에서 삭제
        }	
    }
    
    // 위치 계산 후 전송
    // 현재 게임의 모든 상태(뱀, 사과, 점수)를 클라이언트에게 전송하기 위해 문자열로 직렬
    // 문자열 구조 : STATE [뱀정보]|[사과정보]|[점수정보]
    // 뱀정보: P1이름:x1,y1,x2,y2(A);P2이름:x1,y1(D);
    // 사과정보: A:x,y
    // 점수정보: S:P1=3,P2=1

    private synchronized String getGameStateString() {
        StringBuilder sb = new StringBuilder("STATE ");
        
        // 뱀 위치 (SnakeInfo.snakes에서 파싱?)
        for (SnakeInfo snake : snakes.values()) {
            sb.append(snake.toString()); // "P1:x1,y1,x2,y2(A);"
        }
        
        // 사과 위치 (GameState.appleX, appleY에서 파싱?)
        // 클라이언트가 20px 단위로 그리므로, 그리드 좌표에 20을 곱해 픽셀 좌표로 보내줌
        // "|" -> 뱀정보와 사과정보를 구분, "A:" -> Apple 의미
        sb.append("|A:").append(apple.x * 20).append(",").append(apple.y * 20);
        
        // 점수 (GameState.rankList에서 파싱?
        // "|S:" -> Score 태그 (이름=점수 형식으로 나열)
        // 점수 순으로 정렬하는 로직 추가하면 좋을듯?
        sb.append("|S:");
        for (SnakeInfo snake : snakes.values()) {
            sb.append(snake.name).append("=").append(snake.score).append(",");
        }
        if (!snakes.isEmpty()) sb.deleteCharAt(sb.length() - 1); // 마지막 콤마 제거

        return sb.toString();
    }
}

