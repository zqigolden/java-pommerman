import core.Game;
import players.*;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.mcts_v2.MCTSParams_V2;
import players.mcts_v2.MCTSPlayer_V2;
import players.rhea.RHEAPlayer;
import players.rhea.utils.Constants;
import players.rhea.utils.RHEAParams;
import utils.*;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;

import static utils.Types.VISUALS;

public class Eval {

    private static void printHelp()
    {
        System.out.println("Usage: java Run [args]");
        System.out.println("\t [arg index = 0] Game Mode. 0: FFA; 1: TEAM");
        System.out.println("\t [arg index = 1] Number of level generation seeds. \"-1\" to execute with the ones from paper (20).");
        System.out.println("\t [arg index = 2] Repetitions per seed [N]. \"1\" for one game only with visuals.");
        System.out.println("\t [arg index = 3] Vision Range [VR]. (0, 1, 2 for PO; -1 for Full Observability)");
        System.out.println("\t [arg index = 4-7] Agents. When in TEAM, agents are mates as indices 4-6, 5-7:");
        System.out.println("\t\t 0 DoNothing");
        System.out.println("\t\t 1 Random");
        System.out.println("\t\t 2 OSLA");
        System.out.println("\t\t 3 SimplePlayer");
        System.out.println("\t\t 4 RHEA 200 itereations, shift buffer, pop size 1, random init, length: 12");
        System.out.println("\t\t 5 MCTS 200 iterations, length: 12");
    }

    public static void main(String[] args) {

        //default
        if(args.length == 0)
            args = new String[]{"0", "10", "4", "-1", "6", "7", "4", "5"};

        if(args.length < 8) {
            printHelp();
            return;
        }

        try {

            Random rnd = new Random();

            // Create players
            ArrayList<Player> players = new ArrayList<>();
            int playerID = Types.TILETYPE.AGENT0.getKey();
            int boardSize = Types.BOARD_SIZE;

            Types.GAME_MODE gMode = Types.GAME_MODE.FFA;
            if(Integer.parseInt(args[0]) == 1)
                gMode = Types.GAME_MODE.TEAM;

            int S = Integer.parseInt(args[1]);
            int N = Integer.parseInt(args[2]);
            Types.DEFAULT_VISION_RANGE = Integer.parseInt(args[3]);

            long seeds[];

            if (S == -1)
            {
                //Special case, these seeds are fixed for the experiments in the paper:
                seeds = new long[] {93988, 19067, 64416, 83884, 55636, 27599, 44350, 87872, 40815,
                        11772, 58367, 17546, 75375, 75772, 58237, 30464, 27180, 23643, 67054, 19508};
            }else
            {
                if(S <= 0)
                    S = 1;

                //Otherwise, all seeds are random
                seeds = new long[S];
                for(int i = 0; i < S; i++)
                    seeds[i] = rnd.nextInt(100000);
            }

            long seed = 0;

            String[] playerStr = new String[4];

            for(int i = 4; i <= 7; ++i) {
                int agentType = Integer.parseInt(args[i]);
                Player p = null;


                switch(agentType) {
                    case 0:
                        p = new DoNothingPlayer(playerID++);
                        playerStr[i-4] = "DoNothing";
                        break;
                    case 1:
                        p = new RandomPlayer(seed, playerID++);
                        playerStr[i-4] = "Random";
                        break;
                    case 2:
                        p = new OSLAPlayer(seed, playerID++);
                        playerStr[i-4] = "OSLA";
                        break;
                    case 3:
                        p = new SimplePlayer(seed, playerID++);
                        playerStr[i-4] = "RuleBased";
                        break;
                    case 4:
                        RHEAParams rheaParams = new RHEAParams();
                        rheaParams.budget_type = Constants.ITERATION_BUDGET;
                        rheaParams.iteration_budget = 200;
                        rheaParams.individual_length = 12;
                        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;
                        rheaParams.mutation_rate = 0.5;

                        p = new RHEAPlayer(seed, playerID++, rheaParams);
                        playerStr[i-4] = "RHEA";
                        break;
                    case 5:
                        MCTSParams mctsParams = new MCTSParams();
                        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
                        mctsParams.num_iterations = 200;
                        mctsParams.rollout_depth = 12;

                        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;
                        p = new MCTSPlayer(seed, playerID++, mctsParams);
                        playerStr[i-4] = "MCTS";
                        break;
                    case 6:
                        p = new RandomPlayerTest(seed, playerID++);
                        playerStr[i-4] = "RandomTest";
                        break;
                    case 7:
                        MCTSParams_V2 mctsParams_v2 = new MCTSParams_V2();
                        mctsParams_v2.stop_type = mctsParams_v2.STOP_ITERATIONS;
                        mctsParams_v2.num_iterations = 200;
                        mctsParams_v2.rollout_depth = 12;
                        mctsParams_v2.uct_method = mctsParams_v2.UCT_UCB1;
                        mctsParams_v2.safe_place_method = mctsParams_v2.SAFE_PLACE_DEFAULT;
                        mctsParams_v2.heuristic_method = mctsParams_v2.CUSTOM_HEURISTIC;
                        mctsParams_v2.uctBias = 1.0;
                        mctsParams_v2.progressiveUnpruningEnable = true;

                        if (args.length > 8){
                            mctsParams_v2.num_iterations = Integer.parseInt(args[8]);
                            mctsParams_v2.rollout_depth = Integer.parseInt(args[9]);
                            switch (args[10]){
                                case "UCT_UCB1":
                                    mctsParams_v2.uct_method = mctsParams_v2.UCT_UCB1; break;
                                case "UCT_UCB1_TURNED":
                                    mctsParams_v2.uct_method = mctsParams_v2.UCT_UCB1_TURNED; break;
                                default:
                                    assert false;
                            }
                            switch (args[11]){
                                case "SAFE_PLACE_DEFAULT":
                                    mctsParams_v2.safe_place_method = mctsParams_v2.SAFE_PLACE_DEFAULT; break;
                                case "SAFE_PLACE_EARLY_STOP":
                                    mctsParams_v2.safe_place_method = mctsParams_v2.SAFE_PLACE_EARLY_STOP; break;
                                case "SAFE_PLACE_ADVANCED":
                                    mctsParams_v2.safe_place_method = mctsParams_v2.SAFE_PLACE_ADVANCED; break;
                                default:
                                    assert false;
                            }
                            switch (args[12]){
                                case "CUSTOM_HEURISTIC":
                                    mctsParams_v2.heuristic_method = mctsParams_v2.CUSTOM_HEURISTIC; break;
                                case "ADVANCED_HEURISTIC":
                                    mctsParams_v2.heuristic_method = mctsParams_v2.ADVANCED_HEURISTIC; break;
                                default:
                                    assert false;
                            }
                            mctsParams_v2.uctBias = Double.parseDouble(args[13]);
                            mctsParams_v2.progressiveUnpruningEnable = Boolean.parseBoolean(args[14]);
                            mctsParams_v2.progressiveUnpruningT = Integer.parseInt(args[15]);
                            mctsParams_v2.progressiveUnpruningKInit = Integer.parseInt(args[16]);
                            mctsParams_v2.progressiveUnpruningA = Double.parseDouble(args[17]);
                            mctsParams_v2.progressiveUnpruningB = Double.parseDouble(args[18]);
                        }
                        Field[] declaredFields = mctsParams_v2.getClass().getDeclaredFields();
                        for (Field field: declaredFields){
                            if (field.canAccess(mctsParams_v2)){
                                System.out.println(field.getName() + " -> " + field.get(mctsParams_v2).toString());
                            }
                        }
                        p = new MCTSPlayer_V2(seed, playerID++, mctsParams_v2);
                        playerStr[i-4] = "MCV2";
                    default:
                        System.out.println("WARNING: Invalid agent ID: " + agentType );
                }

                players.add(p);
            }

            String gameIdStr = "";
            for(int i = 0; i <= 7; ++i) {
                gameIdStr += args[i];
                if(i != 7)
                    gameIdStr+="-";
            }

            Game game = new Game(seeds[0], boardSize, gMode, gameIdStr);

            // Make sure we have exactly NUM_PLAYERS players
            assert players.size() == Types.NUM_PLAYERS;
            game.setPlayers(players);

            System.out.print(gameIdStr + " [");
            for(int i = 0; i < playerStr.length; ++i) {
                System.out.print(playerStr[i]);
                if(i != playerStr.length-1)
                    System.out.print(',');

            }
            System.out.println("]");

            runGames(game, seeds, N, false);
        } catch(Exception e) {
            e.printStackTrace();
            printHelp();
        }
    }

    /**
     * Runs 1 game.
     * @param g - game to run
     * @param ki1 - primary key controller
     * @param ki2 - secondary key controller
     * @param separateThreads - if separate threads should be used for the agents or not.
     */
    public static void runGame(Game g, KeyController ki1, KeyController ki2, boolean separateThreads) {
        WindowInput wi = null;
        GUI frame = null;
        if (VISUALS) {
            frame = new GUI(g, "Java-Pommerman", ki1, false, true);
            wi = new WindowInput();
            wi.windowClosed = false;
            frame.addWindowListener(wi);
            frame.addKeyListener(ki1);
            frame.addKeyListener(ki2);
        }

        g.run(frame, wi, separateThreads);
    }

    public static void runGames(Game g, long seeds[], int repetitions, boolean useSeparateThreads){
        int numPlayers = g.getPlayers().size();
        int[] winCount = new int[numPlayers];
        int[] tieCount = new int[numPlayers];
        int[] lossCount = new int[numPlayers];

        int[] overtimeCount = new int[numPlayers];

        int numSeeds = seeds.length;
        int totalNgames = numSeeds * repetitions;

        for(int s = 0; s<numSeeds; s++) {
            long seed = seeds[s];

            for (int i = 0; i < repetitions; i++) {
                long playerSeed = System.currentTimeMillis();

                System.out.print( playerSeed + ", " + seed + ", " + (s*repetitions + i) + "/" + totalNgames + ", ");

                g.reset(seed);
                EventsStatistics.REP = i;
                GameLog.REP = i;

                // Set random seed for players and reset them
                ArrayList<Player> players = g.getPlayers();
                for (int p = 0; p < g.nPlayers(); p++) {
                    players.get(p).reset(playerSeed, p + Types.TILETYPE.AGENT0.getKey());
                }
                Types.RESULT[] results = g.run(useSeparateThreads);

                for (int pIdx = 0; pIdx < numPlayers; pIdx++) {
                    switch (results[pIdx]) {
                        case WIN:
                            winCount[pIdx]++;
                            break;
                        case TIE:
                            tieCount[pIdx]++;
                            break;
                        case LOSS:
                            lossCount[pIdx]++;
                            break;
                    }
                }

                int[] overtimes = g.getPlayerOvertimes();
                for(int j = 0; j < overtimes.length; ++j)
                    overtimeCount[j] += overtimes[j];

            }
        }

        //Done, show stats
        System.out.println("N \tWin \tTie \tLoss \tPlayer (overtime average)");
        for (int pIdx = 0; pIdx < numPlayers; pIdx++) {
            String player = g.getPlayers().get(pIdx).getClass().toString().replaceFirst("class ", "");

            double winPerc = winCount[pIdx] * 100.0 / (double)totalNgames;
            double tiePerc = tieCount[pIdx] * 100.0 / (double)totalNgames;
            double lossPerc = lossCount[pIdx] * 100.0 / (double)totalNgames;
            double overtimesAvg = overtimeCount[pIdx] / (double)totalNgames;

            System.out.println(totalNgames + "\t" + winPerc + "%\t" + tiePerc + "%\t" + lossPerc + "%\t" + player + " (" + overtimesAvg + ")" );
        }
    }
}
