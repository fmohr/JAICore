package ai.libs.jaicore.search.exampleproblems.lake;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.libs.jaicore.search.probleminputs.AMDP;

public class LakeMDP extends AMDP<TimedLakeState, ELakeActions, Double> {

	private static final Collection<ELakeActions> POSSIBLE_ACTIONS = Arrays.asList(ELakeActions.values());

	private final LakeLayout layout;
	private final int goalRow;
	private final int goalCol;
	private final int timeout;
	private final boolean timed;

	public LakeMDP(final LakeLayout layout, final int startRow, final int startCol, final int goalRow, final int goalCol, final int timeout) {
		super(new TimedLakeState(layout, startRow, startCol, 0));
		this.layout = layout;
		this.goalRow = goalRow;
		this.goalCol = goalCol;
		this.timeout = timeout;
		this.timed = timeout > 0;
	}

	@Override
	public Collection<ELakeActions> getApplicableActions(final TimedLakeState state) {
		if ((this.timed && state.getTime() >= this.timeout) || state.isInPit() || this.isGoalState(state)) {
			return Arrays.asList();
		}
		return POSSIBLE_ACTIONS;
	}

	@Override
	public Map<TimedLakeState, Double> getProb(final TimedLakeState state, final ELakeActions action) {
		Map<TimedLakeState, Double> dist = new HashMap<>();
		List<TimedLakeState> possibleOutcomes = new ArrayList<>(3);
		switch (action) {
		case UP:
			possibleOutcomes.add(this.left(state));
			possibleOutcomes.add(this.up(state));
			possibleOutcomes.add(this.right(state));
			break;
		case RIGHT:
			possibleOutcomes.add(this.up(state));
			possibleOutcomes.add(this.right(state));
			possibleOutcomes.add(this.down(state));
			break;
		case DOWN:
			possibleOutcomes.add(this.left(state));
			possibleOutcomes.add(this.down(state));
			possibleOutcomes.add(this.right(state));
			break;
		case LEFT:
			possibleOutcomes.add(this.left(state));
			possibleOutcomes.add(this.up(state));
			possibleOutcomes.add(this.down(state));
			break;
		default:
			throw new IllegalArgumentException("Unknown action " + action);
		}

		/* compute frequencies for the different outcomes */
		for (TimedLakeState succ : possibleOutcomes) {
			dist.put(succ, dist.computeIfAbsent(succ, s -> 0.0) + 1.0 / 3);
		}
		return dist;
	}

	public boolean isGoalState(final LakeState s) {
		return (s.getRow() == this.goalRow && s.getCol() == this.goalCol);
	}

	public TimedLakeState up(final TimedLakeState s) {
		return new TimedLakeState(s.getLayout(), Math.max(0, s.getRow() - 1), s.getCol(), this.timed ? s.getTime() + 1 : s.getTime());
	}

	public TimedLakeState down(final TimedLakeState s) {
		return new TimedLakeState(s.getLayout(), Math.min(this.layout.getRows() - 1, s.getRow() + 1), s.getCol(), this.timed ? s.getTime() + 1 : s.getTime());
	}

	public TimedLakeState left(final TimedLakeState s) {
		return new TimedLakeState(s.getLayout(), s.getRow(), Math.max(0, s.getCol() - 1), this.timed ? s.getTime() + 1 : s.getTime());
	}

	public TimedLakeState right(final TimedLakeState s) {
		return new TimedLakeState(s.getLayout(), s.getRow(), Math.min(this.layout.getCols() - 1, s.getCol() + 1), this.timed ? s.getTime() + 1 : s.getTime());
	}

	@Override
	public Double getScore(final TimedLakeState state, final ELakeActions action, final TimedLakeState successor) {
		if (this.timed) {
			return successor.isInPit() ? (1 - state.getTime() * 1.0 / this.timeout) : 1.0 / this.timeout; // every move gets more expensive than the previous one
		}
		else {
			return this.isGoalState(successor) ? 1.0 : 0.0;
		}
	}

	@Override
	public boolean isMaximizing() {
		return !this.timed; // when timed, we try to get to the other side as quick as possible
	}
}
