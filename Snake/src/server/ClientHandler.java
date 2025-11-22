package server;

import java.io.*;
import java.net.Socket;

// 각 클라이언트 통신 전담 스레드 
class ClientHandler extends Thread {
    
    private Socket socket;          
    private ServerMain server;     
    private GameLogic gamelogic;
    
    private BufferedReader in;
    private PrintWriter out;
    
    private String clientName = "Unknown"; // 플레이어 닉네임

    public ClientHandler(Socket socket, ServerMain server, GameLogic gamelogic) {
        this.socket = socket;
        this.server = server;
        this.gamelogic = gamelogic; // GameLogic 객체 전달받음
        
        try {
            // 스트림 초기화 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); // true: autoFlush
        } catch (IOException e) {
            System.out.println("스트림 초기화 오류: " + e.getMessage());
        }
    }
    
    // 소켓을 강제로 닫는 메소드
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close(); // 소켓을 닫으면 readLine()에서 예외가 발생하며 run()이 종료됨
            }
        } catch (IOException e) {
            System.out.println("소켓 종료 중 오류: " + e.getMessage());
        }
    }

    // 클라이언트로부터 메시지 수신
    @Override
    public void run() {
        try {
            // 첫 번째 메시지는 "JOIN 닉네임" 프로토콜로 처리
            String line = in.readLine(); 
            
            if (line != null && line.startsWith("JOIN ")) {
            	this.clientName = line.substring(5).trim();
            	
            	gamelogic.addPlayer(clientName, this);
                
                System.out.println("[" + clientName + "] 님이 입장했습니다.");
                // 모든 클라이언트에게 입장 메시지 전송 (클라이언트는 "CHAT "으로 시작하는 메시지 파싱)
                server.broadcast("CHAT [" + clientName + "] 님이 입장했습니다.");
            } else {
                System.out.println("프로토콜 오류: JOIN 메시지 필요.");
                return; // 스레드 종료
            }
            
            // 이후 메시지는 "MOVE" 또는 "CHAT"으로 간주하고 계속 수신
            while ((line = in.readLine()) != null) { // readLine() 대기
                
                if (line.startsWith("MOVE ")) {
                	String direction = line.substring(5).trim();
                    // GameLogic에 방향만 설정 (Broadcast 안함)
                    gamelogic.setDirection(clientName, direction);
                
                } else if (line.startsWith("CHAT ")) {
                    // 채팅 메시지 중계
                    String chatMsg = line.substring(5);
                    server.broadcast("CHAT [" + clientName + "]: " + chatMsg);
                }
            }

        } catch (IOException e) {
            // 클라이언트 접속 종료 (정상 또는 비정상)
        	System.out.println(clientName + " 연결 종료됨.");
        } finally {
            // 종료 처리
            System.out.println("[" + clientName + "] 님의 연결이 끊어졌습니다.");
            
            gamelogic.removePlayer(clientName); // GameLogic에서 플레이어 제거
            
            server.removeClient(this); 
            server.broadcast("CHAT [" + clientName + "] 님이 퇴장했습니다.");
            
            // 소켓 및 스트림 닫기
            try {
                if(out != null) out.close();
                if(in != null) in.close();
                if(socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // 외부에서 클라이언트를 알 수 있도록 함
    public String getClientName() {
        return this.clientName;
    }

    // 현재 클라이언트에게 메시지 전송 (서버 -> 클라이언트)
    public void sendMessage(String message) {
        try {
            if (out != null) {
                out.println(message); // autoFlush=true이므로 flush() 불필요
            }
        } catch (Exception e) {
            System.out.println("[" + clientName + "]에게 메시지 전송 오류: " + e.getMessage());
        }
    }
}