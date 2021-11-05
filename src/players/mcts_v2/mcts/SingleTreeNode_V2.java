package players.mcts_v2.mcts;

import core.GameState;
import players.careful.CarefulMoveFilter;
import players.heuristics.AdvancedHeuristic;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.ElapsedCpuTimer;
import utils.Types;
import utils.Utils;
import utils.Vector2d;

import java.util.*;
import java.util.stream.Collectors;

public class SingleTreeNode_V2
{
    public MCTSParams_V2 params;

    private SingleTreeNode_V2 parent;
    private SingleTreeNode_V2[] children;
    private double totValue;
    private int nVisits;
    private Random m_rnd;
    private int m_depth;
    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    private int childIdx;
    private int fmCallsCount;

    private int num_actions;
    private Types.ACTIONS[] actions;

    private GameState rootState;
    private StateHeuristic rootStateHeuristic;

    private Comparator<SingleTreeNode_V2> comparator = new NodeComparator();

    SingleTreeNode_V2(MCTSParams_V2 p, Random rnd, int num_actions, Types.ACTIONS[] actions) {
        this(p, null, -1, rnd, num_actions, actions, 0, null);
    }

    private SingleTreeNode_V2(MCTSParams_V2 p, SingleTreeNode_V2 parent, int childIdx, Random rnd, int num_actions,
                              Types.ACTIONS[] actions, int fmCallsCount, StateHeuristic sh) {
        this.params = p;
        this.fmCallsCount = fmCallsCount;
        this.parent = parent;
        this.m_rnd = rnd;
        this.num_actions = num_actions;
        this.actions = actions;
        children = new SingleTreeNode_V2[num_actions];
        totValue = 0.0;
        this.childIdx = childIdx;
        if(parent != null) {
            m_depth = parent.m_depth + 1;
            this.rootStateHeuristic = sh;
        }
        else
            m_depth = 0;
    }

    void setRootGameState(GameState gs)
    {
        this.rootState = gs;
        if (params.heuristic_method == params.CUSTOM_HEURISTIC)
            this.rootStateHeuristic = new CustomHeuristic(gs);
        else if (params.heuristic_method == params.ADVANCED_HEURISTIC) // New method: combined heuristics
            this.rootStateHeuristic = new AdvancedHeuristic(gs, m_rnd);
    }


    void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;

        int remainingLimit = 5;
        boolean stop = false;

        while(!stop){

            GameState state = rootState.copy();
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            SingleTreeNode_V2 selected = treePolicy(state);
            double delta = selected.rollOut(state);
            backUp(selected, delta);

            //Stopping condition
            if(params.stop_type == params.STOP_TIME) {
                numIters++;
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            }else if(params.stop_type == params.STOP_ITERATIONS) {
                numIters++;
                stop = numIters >= params.num_iterations;
            }else if(params.stop_type == params.STOP_FMCALLS)
            {
                fmCallsCount+=params.rollout_depth;
                stop = (fmCallsCount + params.rollout_depth) > params.num_fmcalls;
            }
        }
        //System.out.println(" ITERS " + numIters);
    }

    private SingleTreeNode_V2 treePolicy(GameState state) {

        SingleTreeNode_V2 cur = this;

        while (!state.isTerminal() && cur.m_depth < params.rollout_depth)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand(state);

            } else {
                cur = cur.uct(state);
            }
        }

        return cur;
    }


    private SingleTreeNode_V2 expand(GameState state) {

        int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        //Roll the state
        roll(state, actions[bestAction]);

        SingleTreeNode_V2 tn = new SingleTreeNode_V2(params,this,bestAction,this.m_rnd,num_actions,
                actions, fmCallsCount, rootStateHeuristic);
        children[bestAction] = tn;
        return tn;
    }

    private void roll(GameState gs, Types.ACTIONS act)
    {
        //Simple, all random first, then my position.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];
        int playerId = gs.getPlayerId() - Types.TILETYPE.AGENT0.getKey();

        for(int i = 0; i < nPlayers; ++i)
        {
            if(playerId == i)
            {
                actionsAll[i] = act;
            }else {
                int actionIdx = m_rnd.nextInt(gs.nActions());
                actionsAll[i] = Types.ACTIONS.all().get(actionIdx);
            }
        }

        gs.next(actionsAll);

    }

    private SingleTreeNode_V2 uct(GameState state) {
        SingleTreeNode_V2 selected = null;
        double bestValue = -Double.MAX_VALUE;
        double sumA = 0, varA = 0, meanA;
        double[] scores;
        double turnedTerm = 1;
        double biasTerm = 0;
        if (params.uct_method == params.UCT_UCB1_TURNED){
            scores = new double[this.children.length];
            int i = 0;
            for (var child: this.children){
                scores[i++] = child.totValue;
                sumA += child.totValue;
            }
            meanA = sumA / this.children.length;
            for (double score : scores) {
                varA += Math.pow(score - meanA, 2);
            }
            varA /= (this.children.length - 1);

        }
        List<SingleTreeNode_V2> avalibleChildren;
        if (params.progressiveUnpruningEnable && nVisits > params.progressiveUnpruningT){
            int k = params.progressiveUnpruningKInit;
            for (int i = 0;;){
                   if (nVisits > params.progressiveUnpruningA * Math.pow(params.progressiveUnpruningB, i++)){
                       k += 1;
                   } else {
                       break;
                   }
            }
            avalibleChildren = Arrays.stream(this.children).sorted(comparator.reversed()).limit(k).collect(Collectors.toList());
        } else {
            avalibleChildren = Arrays.stream(this.children).collect(Collectors.toList());
        }

        for (SingleTreeNode_V2 child : avalibleChildren)
        {
            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + params.epsilon);

            childValue = Utils.normalise(childValue, bounds[0], bounds[1]);

            if (params.uct_method == params.UCT_UCB1_TURNED){
                turnedTerm = Math.min(0.25, varA + (2 * Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon)));
            }
            if (params.uctBias > 0){
                biasTerm = childValue / (1 + (child.nVisits + params.epsilon)) * params.uctBias;
            }

            double uctValue = childValue + params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon) * turnedTerm + biasTerm);

            uctValue = Utils.noise(uctValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " +
                    + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        roll(state, actions[selected.childIdx]);

        return selected;
    }

    private double rollOut(GameState state)
    {
        int thisDepth = this.m_depth;

        while (!finishRollout(state,thisDepth)) {
            int action = safeRandomAction(state);
            roll(state, actions[action]);
            thisDepth++;
        }

        return rootStateHeuristic.evaluateState(state);
    }

    private int safeRandomAction(GameState state)
    {
        if (params.safe_place_method == params.SAFE_PLACE_ADVANCED ||
                (params.safe_place_method == params.SAFE_PLACE_EARLY_STOP && state.getAliveAgentIDs().length > 2)) {
            ArrayList<Types.ACTIONS> filted_actions = CarefulMoveFilter.get_filtered_actions(state);
            Types.ACTIONS rand_act = filted_actions.get(m_rnd.nextInt(filted_actions.size()));
            for (int i = 0; i < actions.length; i++) {
                if (rand_act == actions[i]) {
                    return i;
                }
            }
        }
        Types.TILETYPE[][] board = state.getBoard();
        ArrayList<Types.ACTIONS> actionsToTry = Types.ACTIONS.all();
        int width = board.length;
        int height = board[0].length;

        while(actionsToTry.size() > 0) {

            int nAction = m_rnd.nextInt(actionsToTry.size());
            Types.ACTIONS act = actionsToTry.get(nAction);
            Vector2d dir = act.getDirection().toVec();

            Vector2d pos = state.getPosition();
            int x = pos.x + dir.x;
            int y = pos.y + dir.y;

            if (x >= 0 && x < width && y >= 0 && y < height)
                if(board[y][x] != Types.TILETYPE.FLAMES)
                    return nAction;

            actionsToTry.remove(nAction);
        }

        //Uh oh...
        return m_rnd.nextInt(num_actions);
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean finishRollout(GameState rollerState, int depth)
    {
        if (depth >= params.rollout_depth)      //rollout end condition.
            return true;

        if (rollerState.isTerminal())               //end of game
            return true;

        return false;
    }

    private void backUp(SingleTreeNode_V2 node, double result)
    {
        SingleTreeNode_V2 n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }
            n = n.parent;
        }
    }


    int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }

        return selected;
    }

    private int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                double childValue = children[i].totValue / (children[i].nVisits + params.epsilon);
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    private boolean notFullyExpanded() {
        for (SingleTreeNode_V2 tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }

    class NodeComparator implements Comparator<SingleTreeNode_V2> {
        @Override
        public int compare(SingleTreeNode_V2 o1, SingleTreeNode_V2 o2) {
            return Double.compare(o1.totValue / (o1.nVisits + params.epsilon), o2.totValue / (o2.nVisits + params.epsilon));
        }
    }
}

