package players;

import core.GameState;
import players.careful.CarefulMoveFilter;
import utils.Types;

import java.util.ArrayList;
import java.util.Random;

public class RandomPlayerTest extends Player {
    private Random random;

    public RandomPlayerTest(long seed, int id) {
        super(seed, id);
        reset(seed, id);
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);
        random = new Random(seed);
    }

    @Override
    public Types.ACTIONS act(GameState gs) {
        ArrayList<Types.ACTIONS> actions = CarefulMoveFilter.get_filtered_actions(gs);
        int actionIdx = random.nextInt(actions.size());
        return actions.get(actionIdx);
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    @Override
    public Player copy() {
        return new RandomPlayerTest(seed, playerID);
    }
}
