package server;

import java.awt.Color;
import java.awt.Point;
import java.util.LinkedList;
import java.util.Random;

// 뱀 한 마리의 데이터(위치, 방향, 점수)를 관리하는 객체 -> 개별로 추적
// (GameLogic이 여러 뱀을 관리하는데, 그 중에서 한 마리)
public class SnakeInfo {
	public String name;
	public LinkedList<Point> body = new LinkedList<>(); // 뱀 몸통 좌표 리스트 (첫번째가 머리)
	public String direction = "RIGHT"; // 현재 이동 방향 (UP, DOWN, LEFT, RIGHT)
	public boolean isAlive = true; // 생존 여부
	public int score = 0; // 점수
	public Color color;


	// 몸 길이 증가 플래그 (사과를 먹은 직후 한번만 true -> 이동 시 꼬리 안자름 (몸길이 +1))
	private boolean justAte = false; 
	
	public SnakeInfo(String name, int startX, int startY) {
		this.name = name;
		
		// 랜덤 색상
	    Random r = new Random();
	    this.color = new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256));
	    
		// 뱀 초기화 (시작 좌표와 기본 길이 3으로 설정)
		// LinkedList 앞이 머리 (addFirst), 뒤가 꼬리
		// 시작 시 오른쪽을 향한 형태
		body.addFirst(new Point(startX, startY)); // 머리
		body.addLast(new Point(startX - 1, startY));
		body.addLast(new Point(startX - 2, startY));
	}
 
	public Point getHead() {
		return body.getFirst();
	}

	// ClientHandler에서 호출됨
	// 뱀이 반대 방향으로 즉시 꺾는 것 방지
	public void setDirection(String newDir) {
		if (newDir.equals("UP") && direction.equals("DOWN")) return;
		if (newDir.equals("DOWN") && direction.equals("UP")) return;
		if (newDir.equals("LEFT") && direction.equals("RIGHT")) return;
		if (newDir.equals("RIGHT") && direction.equals("LEFT")) return;
		this.direction = newDir;
	}

	// 이동 로직
	// GameLogic.updateGame()에 호출됨
	// 설정된 'direction'에 따라 뱀을 한칸씩 이동
	public void move() {
		if (!isAlive) return; // 죽은 뱀은 움직이지 않음
     
		Point newHead = new Point(getHead()); // 현재 머리 위치 복사
		switch (direction) {
         	case "UP": newHead.y--; break;
         	case "DOWN": newHead.y++; break;
         	case "LEFT": newHead.x--; break;
         	case "RIGHT": newHead.x++; break;
		}
     
	     body.addFirst(newHead); // 새 머리 추가
	     
	     if (justAte) {
	         // 몸 길이 증가
	         // 사과를 방금 먹었다면, 꼬리를 자르지 않음 (몸 길이 +1)
	         justAte = false; 
	     } else {
	         // 사과를 안 먹었으면, 꼬리 1칸 제거
	         body.removeLast(); 
	     }
	 }
	 
	 // 사과 먹었는지 판정 / 사과 점수 시스템
	 // 사과를 먹었을 때 호출됩
	 public void eat() {
	     this.score++; // 점수 +1
	     this.justAte = true; // 몸 길이 증가 플래그 설정
	 }
	
	 // 킬 점수 시스템
	 // 다른 뱀을 죽였을 때 호출
	 public void addKillScore() {
	     this.score += 5; // 킬 점수 +5
	 }
	 
	// 킬 점수 획득 시 몸도 길어지도록 설정
	 public void grow(int length) {
	     for (int i = 0; i < length; i++) {
	         body.addLast(new Point(body.getLast())); // 꼬리 마지막 위치 복제 → 길이 +n
	     }
	 }
	
	 // 충돌 판정
	 // 뱀 죽음 처리 로직
	 public void die() {
	     this.isAlive = false;
	 }
	 
	 // 충돌 판정 (자기 몸통 충돌)
	 // 뱀의 머리가 자기 몸통(머리 제외)과 겹치는지 확인합니다.
	 public boolean checkSelfCollision() {
	     Point head = getHead();
	     // 머리를 제외한 몸통과 머리가 겹치는지 확인 (1번 인덱스부터)
	     for (int i = 1; i < body.size(); i++) {
	         if (head.equals(body.get(i))) {
	             return true;
	         }
	     }
	     return false;
	 }
	 
	 // 충돌 판정 (다른 뱀 몸통 충돌)
	 public boolean checkBodyCollision(Point otherHead) {
	     // 내 몸통 전체(머리 포함)와 다른 뱀의 머리가 겹치면 true
	     for (Point part : body) {
	         if (otherHead.equals(part)) {
	             return true; // 충돌
	         }
	     }
	     return false;
	 }
	
	 // 위치 전송
	 // 클라이언트에 전송할 이 뱀의 상태 문자열로 변환
	 // ex) "Player1:10,10,10,11,10,12(A);" (A=Alive, D=Dead)
	 @Override
	 public String toString() {
	     StringBuilder sb = new StringBuilder(name + ":");
	     
	     for (Point p : body) {
	         // 팀원의 GamePanel이 20px 단위로 그리므로, 좌표를 *20 해서 전송
	         sb.append(p.x * 20).append(",").append(p.y * 20).append(",");
	     }
	     if (!body.isEmpty()) sb.deleteCharAt(sb.length() - 1); // 마지막 콤마 제거
	     
	     sb.append(isAlive ? "(A)" : "(D)"); // 생사 여부 (A=Alive, D=Dead)
	     
	     sb.append("[")
	      .append(color.getRed()).append(",")
	      .append(color.getGreen()).append(",")
	      .append(color.getBlue())
	      .append("];");
	     
	     return sb.toString();
	 }
}
