package client;

public interface GameStateListener {
	// 서버에서 새로운 상태 또는 채팅 메시지가 오면 NetworkClient -> GamePanel로 전달하기 위한 콜백 시스템이 필요
    void onGameStateUpdated(GameState state);
    default void onChatMessage(String msg) {};
    default void onGameOver() {};
}
