package ai.libs.jaicore.search.algorithms.standard.bestfirst.npuzzle.parentdiscarding;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.libs.jaicore.search.exampleproblems.npuzzle.parentdiscarding.PDPuzzleNode;

public class PDPuzzleNodeTester {
	private PDPuzzleNode n;
	private PDPuzzleNode n1;
	private PDPuzzleNode n2;

	@BeforeEach
	public void before() {
		int[][] board = { { 0, 1 }, { 1, 1 } };
		this.n = new PDPuzzleNode(board, 0, 0);
		board = new int[][] { { 1, 1 }, { 1, 0 } };
		this.n1 = new PDPuzzleNode(board, 1, 1);
		this.n2 = new PDPuzzleNode(board, 1, 1);

	}

	@Test
	public void testGetDistance() {
		assertEquals(2, this.n.getDistance(), 0);
		assertEquals(0, this.n1.getDistance(), 0);
		assertEquals(this.n1, this.n2);
		assertEquals(this.n1.hashCode(), this.n2.hashCode());
	}
}
