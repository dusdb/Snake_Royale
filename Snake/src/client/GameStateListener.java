package client;

public interface GameStateListener {
    void onGameStateUpdated(GameState state);
    default void onChatMessage(String msg) {};
}
