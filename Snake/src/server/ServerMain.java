package server;

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
