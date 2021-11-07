package players.mcts_v2.careful;


import core.GameState;
import utils.Pair;
import utils.Types;
import utils.Utils;
import utils.Vector2d;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CarefulMoveFilter {
    final static int INT_MAX = 9999;
    //#3 options, simple | simple_ajdacent | lookahead
    final static String BOMBING_TEST = "lookahead";
    final static boolean NO_KICKING = true;
    final static int FLAME_LIFE = 2;
    final static double EPSILON = 0.001;
    final static Types.DIRECTIONS[] DIRS_WO_STOP = {
        Types.DIRECTIONS.LEFT,
        Types.DIRECTIONS.RIGHT,
        Types.DIRECTIONS.UP,
        Types.DIRECTIONS.DOWN
    };

    private static Types.DIRECTIONS oppo_direction(Types.DIRECTIONS dir) {
        if (dir == Types.DIRECTIONS.LEFT) {
            return Types.DIRECTIONS.RIGHT;
        }
        if (dir == Types.DIRECTIONS.RIGHT) {
            return Types.DIRECTIONS.LEFT;
        }
        if (dir == Types.DIRECTIONS.UP) {
            return Types.DIRECTIONS.DOWN;
        }
        if (dir == Types.DIRECTIONS.DOWN) {
            return Types.DIRECTIONS.UP;
        }
        return null;
    }



    private static boolean is_moving_direction(GameState prevGs, GameState gs, Vector2d bomb_pos, Types.DIRECTIONS direction){
        Types.DIRECTIONS rev_d = oppo_direction(direction);
        Vector2d rev_pos = Utils.getNextPosition(bomb_pos, rev_d);
        if (! Utils.positionOnBoard(prevGs.getBoard(), rev_pos)){
            return false;
        }
        if (prevGs.getBombLife()[rev_pos.y][rev_pos.x] - 1 == gs.getBombLife()[bomb_pos.y][bomb_pos.x]
                && prevGs.getBombBlastStrength()[rev_pos.y][rev_pos.x] == gs.getBombBlastStrength()[bomb_pos.y][bomb_pos.x]
                && position_is_passage(prevGs.getBoard(), bomb_pos)){
            return true;
        }
        return false;
    }

    private static GameState move_moving_bombs_to_next_position(GameState prev_obs, GameState obs){
        int[][] obsBombLife = obs.getBombLife();
        ArrayList<Vector2d> bombs = new ArrayList<>();
        for (int i = 0; i < obsBombLife.length; i++){
            for (int j = 0; j < obsBombLife[i].length; j++){
                if (obsBombLife[i][j] == 0){
                    continue;
                }
                if (obsBombLife[i][j] > 1){
                    bombs.add(new Vector2d(i, j));
                }
            }
        }
        ArrayList<Pair<Vector2d, Types.DIRECTIONS>> moving_bombs = new ArrayList<>();
        for (Vector2d bomb_pos: bombs){
            Types.DIRECTIONS moving_dir = null;
            for (Types.DIRECTIONS d: Types.DIRECTIONS.values()) {
                if (d == Types.DIRECTIONS.NONE) {
                    continue;
                }
                if (is_moving_direction(prev_obs, obs, bomb_pos, d)) {
                    moving_dir = d;
                    break;
                }
            }
            if (moving_dir != null){
                moving_bombs.add(new Pair<>(bomb_pos, moving_dir));
            }
        }
        Types.TILETYPE[][] board = obs.getBoard();
        int[][] bombLife = obs.getBombLife();
        int[][] bombBlastStrength = obs.getBombBlastStrength();
        for (Pair<Vector2d, Types.DIRECTIONS> moving_bomb : moving_bombs) {
            Vector2d bomb_pos = moving_bomb.first;
            Types.DIRECTIONS moving_dir = moving_bomb.second;
            Vector2d nextPosition = Utils.getNextPosition(bomb_pos, moving_dir);
            if (! Utils.positionOnBoard(board, nextPosition)){
                continue;
            }
            if (position_is_passage(board, nextPosition)){
                board[nextPosition.y][nextPosition.x] = board[bomb_pos.y][bomb_pos.x];
                bombLife[nextPosition.y][nextPosition.x] = bombLife[bomb_pos.y][bomb_pos.x];
                bombBlastStrength[nextPosition.y][nextPosition.x] = bombBlastStrength[bomb_pos.y][bomb_pos.x];
                board[bomb_pos.y][bomb_pos.x] = Types.TILETYPE.PASSAGE;
                bombLife[bomb_pos.y][bomb_pos.x] = 0;
                bombBlastStrength[bomb_pos.y][bomb_pos.x] = 0;
            }
        }
        return obs;
    }

    private static int manhattan_distance(Vector2d pos1, Vector2d pos2) {
        return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y - pos2.y);
    }

    private static int[][] all_bomb_real_life(Types.TILETYPE[][] board, int[][] bomb_life, int[][] bomb_blast_st){
        int[][] bomb_real_life_map = bomb_life.clone();
        int sz = board.length;
        while (true){
            ArrayList<Boolean> no_change = new ArrayList<Boolean>();
            for (int i = 0; i < sz; i++){
                for (int j = 0; j < sz; j++) {
                    if (board[j][i] == Types.TILETYPE.RIGID
                    || board[j][i] == Types.TILETYPE.WOOD
                    || board[j][i] == Types.TILETYPE.EXTRABOMB
                    || board[j][i] == Types.TILETYPE.INCRRANGE
                    || board[j][i] == Types.TILETYPE.KICK
                    || board[j][i] == Types.TILETYPE.FOG){
                        continue;
                    }
                    if (bomb_life[j][i] == 0){
                        continue;
                    }

                    int min_life = bomb_real_life_map[j][i];
                    for (Types.DIRECTIONS direction : DIRS_WO_STOP) {
                       Vector2d pos = new Vector2d(i, j);
                       Vector2d last_pos = new Vector2d(i, j);
                       while (true){
                           pos = Utils.getNextPosition(pos, direction);
                           if (stop_condition(board, pos, true)){
                               break;
                           }
                           if (bomb_real_life_map[pos.y][pos.x] > 0) {
                               if (bomb_real_life_map[pos.y][pos.x] < min_life && manhattan_distance(pos, last_pos) <= bomb_blast_st[pos.y][pos.x] - 1){
                                   min_life = bomb_real_life_map[pos.y][pos.x];
                                   last_pos = pos.copy();
                               }
                               else {
                                   break;
                               }
                           }
                       }
                    }
                    int real_life = min_life;
                    no_change.add(bomb_real_life_map[j][i] == real_life);
                    bomb_real_life_map[j][i] = real_life;


                }
            }
            if (no_change.stream().allMatch(n->n)){
                break;
            }
        }
        return bomb_real_life_map;
    }

    private static boolean position_is_agent(Types.TILETYPE[][] board, Vector2d pos){
        if (board[pos.y][pos.x] == Types.TILETYPE.AGENT0
                || board[pos.y][pos.x] == Types.TILETYPE.AGENT1
                || board[pos.y][pos.x] == Types.TILETYPE.AGENT2
                || board[pos.y][pos.x] == Types.TILETYPE.AGENT3 ) {
            return true;
        }
        return false;
    }

    private static boolean position_is_passage(Types.TILETYPE[][] board, Vector2d pos){
        if (board[pos.y][pos.x] == Types.TILETYPE.PASSAGE) {
            return true;
        }
        return false;
    }

    private static boolean position_is_fog(Types.TILETYPE[][] board, Vector2d pos){
        if (board[pos.y][pos.x] == Types.TILETYPE.FOG) {
            return true;
        }
        return false;
    }

    private static boolean position_is_powerup(Types.TILETYPE[][] board, Vector2d pos){
        if (board[pos.y][pos.x] == Types.TILETYPE.KICK
                || board[pos.y][pos.x] == Types.TILETYPE.INCRRANGE
                || board[pos.y][pos.x] == Types.TILETYPE.EXTRABOMB) {
            return true;
        }
        return false;
    }

    private static boolean position_is_wall(Types.TILETYPE[][] board, Vector2d pos){
        if (board[pos.y][pos.x] == Types.TILETYPE.RIGID
                || board[pos.y][pos.x] == Types.TILETYPE.WOOD) {
            return true;
        }
        return false;
    }

    private static boolean stop_condition(Types.TILETYPE[][] board, Vector2d pos){
        return stop_condition(board, pos, true);
    }

    private static boolean stop_condition(Types.TILETYPE[][] board, Vector2d pos, boolean exclude_agent){
        if (!Utils.positionOnBoard(board, pos)
                || position_is_fog(board, pos)
                || position_is_wall(board, pos)){
            return true;
        }
        if (!exclude_agent && position_is_agent(board, pos)){
            return true;
        }
        return false;
    }

    private static boolean check_if_flame_will_gone(GameState obs, ArrayList<GameState> prev_two_obs, Vector2d flame_pos){
        if (!(prev_two_obs.get(0).getBoard()[flame_pos.y][flame_pos.x] == Types.TILETYPE.FLAMES
        && prev_two_obs.get(1).getBoard()[flame_pos.y][flame_pos.x] == Types.TILETYPE.FLAMES)){
            return false;
        }
        Types.TILETYPE[][] board = obs.getBoard();
        LinkedList<Vector2d> Q = new LinkedList<>();
        Q.push(flame_pos);
        HashSet<Vector2d> visited = new HashSet<>();
        visited.add(flame_pos);
        while (Q.size() > 0){
            Vector2d pos = Q.pop();
            if (!(prev_two_obs.get(0).getBoard()[flame_pos.y][flame_pos.x] == Types.TILETYPE.FLAMES
                    && prev_two_obs.get(1).getBoard()[flame_pos.y][flame_pos.x] == Types.TILETYPE.FLAMES)){
                return false;
            }
            for (Types.DIRECTIONS d: DIRS_WO_STOP){
                Vector2d next_pos = Utils.getNextPosition(pos, d);
                if (Utils.positionOnBoard(board, next_pos) && position_is_agent(board, next_pos)){
                    if (!visited.contains(next_pos)){
                        Q.add(next_pos);
                        visited.add(next_pos);
                    }
                }
            }
        }
        return true;

    }

    private static Pair<Boolean, Pair<Integer, Integer>> position_covered_by_bomb(GameState obs, Vector2d pos, int[][] bomb_real_life_map){
        Vector2d min_bomb_pos = null;
        Vector2d max_bomb_pos = null;
        int min_bomb_value = INT_MAX;
        int max_bomb_value = -INT_MAX;
        if (obs.getBombLife()[pos.y][pos.x] > 0){
            min_bomb_value = bomb_real_life_map[pos.y][pos.x];
            max_bomb_value = bomb_real_life_map[pos.y][pos.x];
            min_bomb_pos = pos;
            max_bomb_pos = pos;
        }
        Types.TILETYPE[][] board = obs.getBoard();
        for (Types.DIRECTIONS d: DIRS_WO_STOP){
            Vector2d next_pos = pos;
            while (true){
                next_pos = Utils.getNextPosition(next_pos, d);
                if (stop_condition(board, next_pos, true)){
                    break;
                }
                if (obs.getBombLife()[next_pos.y][next_pos.x] > 0
                        && obs.getBombBlastStrength()[next_pos.y][next_pos.x] - 1 >= manhattan_distance(pos, next_pos)){
                    if (bomb_real_life_map[next_pos.y][next_pos.x] < min_bomb_value){
                        min_bomb_value = bomb_real_life_map[next_pos.y][next_pos.x];
                        min_bomb_pos = next_pos;
                    }
                    if (bomb_real_life_map[next_pos.y][next_pos.x] > max_bomb_value){
                        max_bomb_value = bomb_real_life_map[next_pos.y][next_pos.x];
                        max_bomb_pos = next_pos;
                    }
                    break;
                }
            }
        }
        if (min_bomb_pos != null){
            return new Pair<>(true, new Pair<>(min_bomb_value, max_bomb_value));
        }
        return new Pair<>(false, new Pair<>(INT_MAX, -INT_MAX));
    }

    private static int compute_min_evade_step(GameState obs, List<Vector2d> parent_pos_list, Vector2d pos, int[][] bomb_real_life){
        Pair<Boolean, Pair<Integer, Integer>> booleanPairPair = position_covered_by_bomb(obs, pos, bomb_real_life);
        boolean flag_cover = booleanPairPair.first;
        int min_cover_value = booleanPairPair.second.first;
        int max_cover_value = booleanPairPair.second.second;
        if (!flag_cover){
            return 0;
        } else if (parent_pos_list.size() >= max_cover_value){
            if (parent_pos_list.size() > max_cover_value + FLAME_LIFE) {
                return 0;
            } else {
                return INT_MAX;
            }
        } else {
            Types.TILETYPE[][] board = obs.getBoard();
            int min_step = INT_MAX;
            for (Types.DIRECTIONS d: DIRS_WO_STOP){
                Vector2d next_pos = Utils.getNextPosition(pos, d);
                if (! Utils.positionOnBoard(board, next_pos) || position_is_powerup(board, next_pos)){
                    continue;
                }
                if (parent_pos_list.contains(next_pos)){
                    continue;
                }
                List<Vector2d> next_pos_list = Stream.concat(parent_pos_list.stream(), Stream.of(next_pos)).toList();
                int x = compute_min_evade_step(obs, next_pos_list, next_pos, bomb_real_life);
                min_step = Integer.min(min_step, x + 1);
            }
            return min_step;
        }
    }

    private static HashSet<Types.ACTIONS> compute_safe_actions(GameState obs, boolean exclude_kicking, ArrayList<GameState> prev_two_obs)
    {
        HashSet<Types.ACTIONS> ret = new HashSet<>();
        Vector2d myPosition = obs.getPosition();
        Types.TILETYPE[][] board = obs.getBoard();
        int[][] bombBlastStrength = obs.getBombBlastStrength();
        int[][] bombLife = obs.getBombLife();
        boolean canKick = obs.canKick();
        Types.DIRECTIONS kick_dir = null;
        int[][] bomb_real_life_map = all_bomb_real_life(board, bombLife, bombBlastStrength);
        ArrayList<Boolean> flag_cover_passages = new ArrayList<>();
        for (Types.DIRECTIONS direction : DIRS_WO_STOP) {
            Vector2d position = Utils.getNextPosition(myPosition, direction);
            if (!Utils.positionOnBoard(board, position)){
                continue;
            }
            if (!exclude_kicking && board[position.y][position.x] == Types.TILETYPE.BOMB && canKick){
                if (kick_test(board, bombBlastStrength, bomb_real_life_map, myPosition, direction)){
                    ret.add(Utils.directionToAction(direction));
                    kick_dir = direction;
                }
            }
            Vector2d gone_flame_pos = null;
            if (prev_two_obs.get(0) != null && prev_two_obs.get(1) != null && check_if_flame_will_gone(obs, prev_two_obs, position)){
                obs.getBoard()[position.y][position.x] = Types.TILETYPE.PASSAGE;
                gone_flame_pos = position;
            }
            if (position_is_passage(board, position) || position_is_powerup(board, position)){
                Types.TILETYPE my_id = board[myPosition.y][myPosition.x];
                if (bombLife[myPosition.y][myPosition.x] > 0) {
                    board[myPosition.y][myPosition.x] = Types.TILETYPE.BOMB;
                } else {
                    board[myPosition.y][myPosition.x] = Types.TILETYPE.PASSAGE;
                }
                Pair<Boolean, Pair<Integer, Integer>> booleanPairPair = position_covered_by_bomb(obs, position, bomb_real_life_map);
                boolean flag_cover = booleanPairPair.first;
                int min_cover_value = booleanPairPair.second.first;
//                int max_cover_value = booleanPairPair.second.second;
                flag_cover_passages.add(flag_cover);
                if (!flag_cover){
                    ret.add(Utils.directionToAction(direction));
                } else {
                    int min_escape_step = compute_min_evade_step(obs, List.of(position), position, bomb_real_life_map);
                    assert (min_escape_step > 0);
                    if (min_escape_step < min_cover_value){
                        ret.add(Utils.directionToAction(direction));
                    }
                }
                obs.getBoard()[position.y][position.x] = my_id;
            }
            if (gone_flame_pos != null){
                obs.getBoard()[gone_flame_pos.y][gone_flame_pos.x] = Types.TILETYPE.FLAMES;
            }
        }
        Types.TILETYPE my_id = obs.getBoard()[myPosition.y][myPosition.x];
        if (bombLife[myPosition.y][myPosition.x] > 0){
            obs.getBoard()[myPosition.y][myPosition.x] = Types.TILETYPE.BOMB;
        } else {
            obs.getBoard()[myPosition.y][myPosition.x] = Types.TILETYPE.PASSAGE;
        }
        Pair<Boolean, Pair<Integer, Integer>> booleanPairPair = position_covered_by_bomb(obs, myPosition, bomb_real_life_map);
        boolean flag_cover = booleanPairPair.first;
        int min_cover_value = booleanPairPair.second.first;
        int max_cover_value = booleanPairPair.second.second;
        if (flag_cover){
            int min_escape_step = compute_min_evade_step(obs, Stream.of(null, myPosition).collect(Collectors.toList()), myPosition, bomb_real_life_map);
            if (min_escape_step < min_cover_value){
                ret.add(Utils.directionToAction(Types.DIRECTIONS.NONE));
            }
        } else {
            ret.add(Utils.directionToAction(Types.DIRECTIONS.NONE));
        }
        obs.getBoard()[myPosition.y][myPosition.x] = my_id;
        if (!(obs.getAmmo() <= 0 || obs.getBombLife()[obs.getPosition().y][obs.getPosition().x] > 0)){
            if (BOMBING_TEST == "simple"){
                if (!flag_cover){
                    ret.add(Types.ACTIONS.ACTION_BOMB);
                }
            } else if (BOMBING_TEST == "simple_adjacent") {
                if (!flag_cover && !flag_cover_passages.contains(true)){
                    ret.add(Types.ACTIONS.ACTION_BOMB);
                }
            } else {
                if (ret.contains(Types.ACTIONS.ACTION_STOP) && ret.size() == 1 && kick_dir == null){
                    GameState obs2 = obs.copy();
                    Vector2d my_pos = obs2.getPosition();
                    obs2.getBoard()[my_pos.y][my_pos.x] = Types.TILETYPE.BOMB;
                    if (flag_cover) {
                        obs2.getBombLife()[my_pos.y][my_pos.x] = min_cover_value;
                    } else {
                        obs2.getBombLife()[my_pos.y][my_pos.x] = 10;
                    }
                    obs2.getBombBlastStrength()[my_pos.y][my_pos.x] = obs2.getBlastStrength();
                    int[][] bomb_life2 = obs2.getBombLife();
                    int[][] bomb_blast_st2 = obs2.getBombBlastStrength();
                    Types.TILETYPE[][] board2 = obs2.getBoard();
                    bomb_real_life_map = all_bomb_real_life(board2, bomb_life2, bomb_blast_st2);
                    int min_evade_step = compute_min_evade_step(obs2, Stream.of(null, myPosition).collect(Collectors.toList()), my_pos, bomb_real_life_map);
                    int current_cover_value = obs2.getBombLife()[my_pos.y][my_pos.x];
                    if (min_evade_step < current_cover_value){
                        ret.add(Types.ACTIONS.ACTION_BOMB);
                    }
                }
            }
        }
        return ret;
    }

    private static boolean moving_bomb_check(Types.TILETYPE[][] board, int[][] blast_st, int[][] bomb_life, Vector2d moving_bomb_pos, Types.DIRECTIONS p_dir, int time_elapsed){
        Vector2d pos2 = Utils.getNextPosition(moving_bomb_pos, p_dir);
        int dist = 0;
        for (int i = 0; i < 10; i++){
            dist++;
            if (! Utils.positionOnBoard(board, pos2)){
                break;
            }
            if (! position_is_powerup(board, pos2) || position_is_passage(board, pos2)){
                break;
            }
            int life_now = bomb_life[pos2.y][pos2.x] - time_elapsed;
            if (bomb_life[pos2.y][pos2.x] > 0 && life_now >= -2 && life_now <= 0 && dist < blast_st[pos2.y][pos2.x]){
                return false;
            }
            pos2 = Utils.getNextPosition(pos2, p_dir);
        }
        return true;
    };

    private static boolean kick_test(Types.TILETYPE[][] board, int[][] blast_st, int[][] bomb_life, Vector2d my_position, Types.DIRECTIONS direction){
        Vector2d next_position = Utils.getNextPosition(my_position, direction);
        assert board[next_position.y][next_position.x] == Types.TILETYPE.BOMB;
        int life_value = bomb_life[next_position.y][next_position.x];
        int strength = blast_st[next_position.y][next_position.x];
        int dist = 0;
        Vector2d pos = Utils.getNextPosition(next_position, direction);
        List<Types.DIRECTIONS> perpendicular_dirs = List.of(Types.DIRECTIONS.LEFT, Types.DIRECTIONS.RIGHT);
        if (direction == Types.DIRECTIONS.LEFT || direction == Types.DIRECTIONS.RIGHT){
            perpendicular_dirs = List.of(Types.DIRECTIONS.DOWN, Types.DIRECTIONS.UP);
        }
        for (int i = 0; i < life_value; i++){
            if (Utils.positionOnBoard(board, pos) && position_is_passage(board, pos)){
                if (!(moving_bomb_check(board, blast_st, bomb_life, pos, perpendicular_dirs.get(0), i)
                        && moving_bomb_check(board, blast_st, bomb_life, pos, perpendicular_dirs.get(1), i))){
                    break;
                }
                dist++;
            } else {
                break;
            }
            pos = Utils.getNextPosition(pos, direction);
        }
        return dist > strength;
    }

    public static ArrayList<Types.ACTIONS> get_filtered_actions(GameState obs){
        ArrayList<GameState> null_pair = new ArrayList<>(){{add(null); add(null);}};
        return get_filtered_actions(obs, null_pair);
    }
    public static ArrayList<Types.ACTIONS> get_filtered_actions(GameState obs, ArrayList<GameState> prev_two_obs) {

        ArrayList<Types.ACTIONS> stop_action = new ArrayList<>();
        stop_action.add(Types.ACTIONS.ACTION_STOP);
        GameState finalObs = obs;
        if (Arrays.stream(obs.getAliveAgentIDs()).noneMatch(n->(n.getKey() == finalObs.getPlayerId()))){
            return stop_action;
        }
        GameState obs_cpy = obs.copy();
        if (prev_two_obs.get(prev_two_obs.size() - 1) != null){
            obs = move_moving_bombs_to_next_position(prev_two_obs.get(prev_two_obs.size() - 1), obs);
        }
        ArrayList<Types.ACTIONS>ret = new ArrayList<>(compute_safe_actions(obs, NO_KICKING, prev_two_obs));
        obs = obs_cpy;
        if (ret.size() > 0){
            return ret;
        }
        return stop_action;
    }
}