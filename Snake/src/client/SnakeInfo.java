package client;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SnakeInfo {
    public String playerName;
    public List<Point> segments = new ArrayList<>(); // 머리부터 꼬리까지 좌표
    public boolean alive = true;
    public int score = 0;

    public Color color = Color.GREEN; // 서버가 색을 보내줘도 되고, 클라가 매핑해도 됨
}
