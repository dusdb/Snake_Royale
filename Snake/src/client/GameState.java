package client;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public List<SnakeInfo> snakes = new ArrayList<>();  // 각 플레이어의 뱀 정보
    public int appleX;          // 사과 X 좌표
    public int appleY;          // 사과 Y 좌표
    public boolean gameOver;    // 게임 종료 여부
    public String winnerName;   // 승자 이름

    // 추가된 필드
    public List<String> rankList = new ArrayList<>();       // 현재 순위 리스트
    public List<String> systemMessages = new ArrayList<>(); // 서버에서 전송하는 시스템 메시지
}
