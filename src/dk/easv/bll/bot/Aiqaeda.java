package dk.easv.bll.bot;

import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import javafx.scene.Node;

import java.util.List;
import java.util.Random;

public class Aiqaeda implements IBot {

    private int previousAction; // the move that led to this state
    private Node parent; // parent node
    private List<Node> children; // child nodes
    private int wins; // number of wins
    private int visits; // number of visits
    private List<Integer> untriedActions; // actions not yet explored
    private int previousPlayer; // the player who made the previous move
    private String BOT_NAME = getClass().getSimpleName();
    private Random random = new Random();

    @Override
    public IMove doMove(IGameState state) {
        return null;
    }

    @Override
    public String getBotName() {
        return BOT_NAME;
    }
}
