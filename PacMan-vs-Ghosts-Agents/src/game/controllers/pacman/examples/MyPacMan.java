package game.controllers.pacman.examples;

import game.PacManSimulator;
import game.controllers.ghosts.game.GameGhosts;
import game.controllers.pacman.PacManHijackController;
import game.core.Game;
import searches.abstraction.IProblem;
import searches.algorithms.UniformCostSearch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class MyPacMan extends PacManHijackController {
    @Override
    public void tick(Game game, long timeDue) {

        GameState gameState = new GameState(game);
        UniformCostSearch<Integer, Integer> searcher = new UniformCostSearch<>(gameState);
        UniformCostSearch<Integer, Integer>.Node result = searcher.search();
        if (result == null) System.out.println("WTF?!");
        if (result.getParent() == null) {
            pacman.set(0);
            return;
        }
        while (result.getParent().getState() != game.getCurPacManLoc()) result = result.getParent();

        pacman.set(result.getAction());

    }

    private class GameState implements IProblem<Integer, Integer> {
        Game game;

        public GameState(Game game) {
            this.game = game;


        }

        @Override
        public Integer initialState() {
            return game.getCurPacManLoc();
        }

        @Override
        public List<Integer> actions(Integer state) {


            int[] dirs = new int[]{game.RIGHT, game.LEFT, game.UP, game.DOWN};
            var ghostIndices = IntStream.of(0, 1, 2, 3).filter(g -> !game.isEdible(g)).map(g -> game.getCurGhostLoc(g)).boxed().collect(Collectors.toCollection(HashSet::new));
            var possibleDirs = Arrays.stream(dirs).boxed().filter(d -> game.getNeighbour(state, d) != game.EMPTY);
            return possibleDirs.filter(i -> !ghostIndices.contains(i)).collect(Collectors.toList());
        }

        @Override
        public Integer result(Integer state, Integer action) {
            return game.getNeighbour(state, action);
        }

        @Override
        public boolean isGoal(Integer state) {
            int pillIndex = game.getPillIndex(state);
            //  boolean isEdibleGhost = (Arrays.stream(new int[]{0, 1, 2, 3}).anyMatch(i -> game.isEdible(i) && game.getPathDistance(game.getCurPacManLoc(), game.getCurGhostLoc(i)) < 50));
            if (pillIndex == -1) {
                int ghostIndex = -1;
                for (int i : new int[]{0, 1, 2, 3}) {
                    if (game.getCurGhostLoc(i) == state) {
                        ghostIndex = i;
                    }
                }
                if (ghostIndex == -1) return false;
                return game.isEdible(ghostIndex);
            }

            return game.checkPill(pillIndex);
        }

        @Override
        public double cost(Integer state, Integer action) {
            int result = result(state, action);
            double cost = 1;

            double factor = 20; //15
            double threshold = 8; //5
            double step = 0.25; //0.25
            for (int i = 0; i < game.NUM_GHOSTS; i++) {
                int pathLen = game.getPathDistance(game.getCurGhostLoc(i), result); //with ghostDist it was too slow

                // if (!game.isEdible(i) && dist.length > pathLen) cost += dist[pathLen];
                if (!game.isEdible(i) && threshold >= pathLen * step)
                    cost += Math.pow(factor, threshold - pathLen * step);
            }
            //if (minGhostDist < game.getPathDistance(game.getCurPacManLoc(), result) && !isEdible) cost *= 5;
            return cost;
        }
    }

    public static void main(String[] args) {
        PacManSimulator.play(new MyPacMan(), new GameGhosts());
    }
}