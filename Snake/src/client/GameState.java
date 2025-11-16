package client;

import java.awt.*;
import java.util.*;

public class GameState {

    // 플레이어 이름 → 뱀 몸 리스트 (픽셀 좌표)
    public Map<String, java.util.List<Point>> snakeBodies = new HashMap<>();

    // 플레이어 이름 → 생존 여부
    public Map<String, Boolean> snakeAlive = new HashMap<>();

    // 플레이어 이름 → 점수
    public Map<String, Integer> scores = new HashMap<>();

    // 사과 좌표
    public int appleX;
    public int appleY;
}
