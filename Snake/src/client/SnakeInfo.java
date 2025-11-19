package client;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SnakeInfo {
    public String playerName;
    
    // 리스트 형태 사용 이유
    	// 뱀은 여러 칸으로 이루어진 객체이기 때문에 전체 몸통 좌표를 저장하기 위해 
    	// 뱀처럼 자연스러운 움직임 구현 목적
    // Point는 하나의 격좌 좌표를 표현(뱀의 한 조각)
    public List<Point> segments = new ArrayList<>(); // 머리부터 꼬리까지 좌표
    public boolean alive = true;
    public int score = 0;

    public Color color = Color.GREEN; // 서버가 색을 보내줘도 되고, 클라가 매핑해도 됨
}
