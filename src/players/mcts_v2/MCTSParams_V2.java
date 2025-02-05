package players.mcts_v2;

import players.optimisers.ParameterSet;
import utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class MCTSParams_V2 implements ParameterSet {

    // Constants
    public final double HUGE_NEGATIVE = -1000;
    public final double HUGE_POSITIVE =  1000;

    public final int STOP_TIME = 0;
    public final int STOP_ITERATIONS = 1;
    public final int STOP_FMCALLS = 2;

    public final int CUSTOM_HEURISTIC = 0;
    public final int ADVANCED_HEURISTIC = 1;

    public double epsilon = 1e-6;

    // Parameters
    public double K = Math.sqrt(2);
    public int rollout_depth = 14;//10;
    public int heuristic_method = CUSTOM_HEURISTIC;

    // Budget settings
    public int stop_type = STOP_TIME;
    public int num_iterations = 300;
    public int num_fmcalls = 2000;
    public int num_time = 40;

    // Custom settings
    public final int SAFE_PLACE_DEFAULT = 0;
    public final int SAFE_PLACE_ADVANCED = 1;
    public final int SAFE_PLACE_EARLY_STOP = 2;
    public int safe_place_method = SAFE_PLACE_DEFAULT;
    public final int UCT_UCB1 = 0;
    public final int UCT_UCB1_TURNED = 1;
    public int uct_method = UCT_UCB1;

    public double uctBias = 0.1;
    public boolean progressiveUnpruningEnable = true;
    public int progressiveUnpruningT = 20;
    public int progressiveUnpruningKInit = 2;
    public double progressiveUnpruningA = 40;
    public double progressiveUnpruningB = 1.2;



    @Override
    public void setParameterValue(String param, Object value) {
        switch(param) {
            case "K": K = (double) value; break;
            case "rollout_depth": rollout_depth = (int) value; break;
            case "heuristic_method": heuristic_method = (int) value; break;
            case "safe_place_method": safe_place_method = (int) value; break;
            case "uct_method": uct_method = (int) value; break;
            case "uct_bias": uctBias = (double) value; break;
        }
    }

    @Override
    public Object getParameterValue(String param) {
        switch(param) {
            case "K": return K;
            case "rollout_depth": return rollout_depth;
            case "heuristic_method": return heuristic_method;
            case "safe_place_method": return safe_place_method;
            case "uct_method": return uct_method;
            case "uct_bias": return uctBias;
        }
        return null;
    }

    @Override
    public ArrayList<String> getParameters() {
        ArrayList<String> paramList = new ArrayList<>();
        paramList.add("K");
        paramList.add("rollout_depth");
        paramList.add("heuristic_method");
        paramList.add("safe_place_method");
        paramList.add("uct_method");
        paramList.add("uct_bias");
        return paramList;
    }

    @Override
    public Map<String, Object[]> getParameterValues() {
        HashMap<String, Object[]> parameterValues = new HashMap<>();
        parameterValues.put("K", new Double[]{1.0, Math.sqrt(2), 2.0});
        parameterValues.put("rollout_depth", new Integer[]{5, 8, 10, 12, 15});
        parameterValues.put("heuristic_method", new Integer[]{CUSTOM_HEURISTIC, ADVANCED_HEURISTIC});
        parameterValues.put("safe_place_method", new Integer[]{SAFE_PLACE_DEFAULT, SAFE_PLACE_ADVANCED, SAFE_PLACE_EARLY_STOP});
        parameterValues.put("uct_method", new Integer[]{UCT_UCB1, UCT_UCB1_TURNED});
        return parameterValues;
    }

    @Override
    public Pair<String, ArrayList<Object>> getParameterParent(String parameter) {
        return null;  // No parameter dependencies
    }

    @Override
    public Map<Object, ArrayList<String>> getParameterChildren(String root) {
        return new HashMap<>();  // No parameter dependencies
    }

    @Override
    public Map<String, String[]> constantNames() {
        HashMap<String, String[]> names = new HashMap<>();
        names.put("heuristic_method", new String[]{"CUSTOM_HEURISTIC", "ADVANCED_HEURISTIC"});
        names.put("safe_place_method", new String[]{"SAFE_PLACE_DEFAULT", "SAFE_PLACE_ADVANCED", "SAFE_PLACE_EARLY_STOP"});
        names.put("uct_method", new String[]{"UCT_UCB1", "UCT_UCB1_TURNED"});
        return names;
    }
}
