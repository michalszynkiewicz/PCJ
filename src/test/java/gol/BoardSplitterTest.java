package gol;

import gol.BoardSplitter.Split;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/6/16
 * Time: 9:04 PM
 */
public class BoardSplitterTest {
    @Test
    public void shouldSplit10x10By1() {
        Split split = split(10, 10, 1);
        assertThat(split.cols).isEqualTo(1);
        assertThat(split.rows).isEqualTo(1);
        assertThat(split.width).isEqualTo(10);
        assertThat(split.height).isEqualTo(10);
    }

    @Test
    public void shouldSplit10x10By2() {
        Split split = split(10, 10, 2);
        assertThat(split.cols).isEqualTo(1);
        assertThat(split.rows).isEqualTo(2);
        assertThat(split.width).isEqualTo(10);
        assertThat(split.height).isEqualTo(5);
    }

    @Test
    public void shouldSplit1000x10By5() {
        Split split = split(1000, 10, 5);
        assertThat(split.cols).isEqualTo(1);
        assertThat(split.rows).isEqualTo(5);
        assertThat(split.width).isEqualTo(10);
        assertThat(split.height).isEqualTo(200);
    }

    @Test
    public void shouldSplit1000x100By5() {
        Split split = split(1000, 100, 5);
        assertThat(split.cols).isEqualTo(1);
        assertThat(split.rows).isEqualTo(5);
        assertThat(split.width).isEqualTo(100);
        assertThat(split.height).isEqualTo(200);
    }
    @Test
    public void shouldSplit1000x100By10() {
        Split split = split(1000, 100, 10);
        assertThat(split.cols).isEqualTo(1);
        assertThat(split.rows).isEqualTo(10);
        assertThat(split.width).isEqualTo(100);
        assertThat(split.height).isEqualTo(100);
    }

    @Test
    public void shouldSplit1000x1000By9() {
        Split split = split(1000, 1000, 9);
        assertThat(split.cols).isEqualTo(3);
        assertThat(split.rows).isEqualTo(3);
        assertThat(split.width).isEqualTo(334);
        assertThat(split.height).isEqualTo(334);
    }

    private Split split(int m, int n, int nodes) {
        return new BoardSplitter(m, n).split(nodes);
    }
}