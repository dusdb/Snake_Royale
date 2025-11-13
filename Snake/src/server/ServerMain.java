package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

// 지렁이 게임 메인 서버
public class ServerMain {

    private ServerSocket serverSocket;
    private int port = 5000; // 서버 포트
    
    // 접속한 모든 ClientHandler를 저장
    private Vector<ClientHandler> clientHandlers = new Vector<>();

    // 서버 시작
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("지렁이 게임 서버가 " + port + " 포트에서 시작되었습니다.");

            // 클라이언트 접속을 항상 기다림
            while (true) {
                System.out.println("클라이언트 접속 대기 중...");
                Socket socket = serverSocket.accept(); 
                System.out.println("클라이언트 접속 성공: " + socket.getInetAddress());

                // 클라이언트별 전담 스레드 생성
                ClientHandler handler = new ClientHandler(socket, this);
                addClient(handler); // 리스트에 추가
                handler.start(); // 스레드 시작
            }

        } catch (IOException e) {
            System.out.println("서버 시작 오류: " + e.getMessage());
        }
    }

    // 모든 클라이언트에게 메시지 전송 (Broadcast)
    public synchronized void broadcast(String message) {
        for (ClientHandler client : clientHandlers) {
            client.sendMessage(message);
        }
    }

    // Vector에 클라이언트 추가 (동기화)
    public synchronized void addClient(ClientHandler client) {
        clientHandlers.add(client);
        System.out.println("새 클라이언트 추가. 현재 인원: " + clientHandlers.size());
    }

    // Vector에서 클라이언트 제거 (동기화)
    public synchronized void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
        System.out.println("클라이언트 퇴장. 현재 인원: " + clientHandlers.size());
    }

    // 메인 메소드
    public static void main(String[] args) {
    		ServerMain server = new ServerMain();
        server.startServer();
    }
}

// 각 클라이언트 통신 전담 스레드 
class ClientHandler extends Thread {
    
    private Socket socket;          
    private ServerMain server;     

    private BufferedReader in;
    private PrintWriter out;

    private String clientName = "Unknown";

    public ClientHandler(Socket socket, ServerMain server) {
        this.socket = socket;
        this.server = server;
        try {
            // 스트림 초기화 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); // true: autoFlush
        } catch (IOException e) {
            System.out.println("스트림 초기화 오류: " + e.getMessage());
        }
    }

    // 클라이언트로부터 메시지 수신
    @Override
    public void run() {
        try {
            // 첫 번째 메시지는 "JOIN 닉네임" 프로토콜로 처리
            String line = in.readLine(); 
            
            if (line != null && line.startsWith("JOIN ")) {
                this.clientName = line.substring("JOIN ".length());
            } else {
                System.out.println("프로토콜 오류: JOIN 메시지 필요.");
                return; // 스레드 종료
            }

            System.out.println("[" + clientName + "] 님이 입장했습니다.");
            // 모든 클라이언트에게 입장 메시지 전송 (클라이언트는 "CHAT "으로 시작하는 메시지 파싱)
            server.broadcast("CHAT [" + clientName + "] 님이 입장했습니다."); 

            // 이후 메시지는 "MOVE" 또는 "CHAT"으로 간주하고 계속 수신
            while ((line = in.readLine()) != null) { // readLine() 대기
                
                if (line.startsWith("MOVE ")) {
                    // 1주차 테스트: 받은 방향키 입력을 다른 클라에게 중계
                    String moveData = line.substring("MOVE ".length());
                    // 2주차 목표: 이 데이터를 기반으로 서버가 게임 상태(GameState)를 계산하여 "STATE ..." 메시지 전송
                    server.broadcast("STATE_UPDATE " + this.clientName + " " + moveData); // (임시 프로토콜)
                
                } else if (line.startsWith("CHAT ")) {
                    // 채팅 메시지 중계
                    String chatMsg = line.substring("CHAT ".length());
                    server.broadcast("CHAT [" + clientName + "]: " + chatMsg);
                }
            }

        } catch (IOException e) {
            // 클라이언트 접속 종료 (정상 또는 비정상)
        } finally {
            // 종료 처리
            System.out.println("[" + clientName + "] 님의 연결이 끊어졌습니다.");
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