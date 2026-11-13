package com.debangshu.dedupe.matching;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnionFindTest {

    @Test
    void matchesAreTransitive() {
        UnionFind groups = new UnionFind(5);

        groups.union(0, 1);
        groups.union(1, 2); // 0 and 2 were never compared, but they are one group now

        assertThat(groups.connected(0, 2)).isTrue();
        assertThat(groups.connected(0, 3)).isFalse();
    }

    @Test
    void unionReportsWhetherAnythingChanged() {
        UnionFind groups = new UnionFind(3);

        assertThat(groups.union(0, 1)).isTrue();
        assertThat(groups.union(1, 0)).isFalse();
    }

    @Test
    void groupsOnlyContainsGroupsOfTwoOrMore() {
        UnionFind groups = new UnionFind(6);
        groups.union(4, 5);
        groups.union(0, 2);
        groups.union(2, 3);

        assertThat(groups.groups()).containsExactly(List.of(0, 2, 3), List.of(4, 5));
    }

    @Test
    void handlesALongChain() {
        int size = 100_000;
        UnionFind groups = new UnionFind(size);
        for (int i = 1; i < size; i++) {
            groups.union(i - 1, i);
        }

        assertThat(groups.find(0)).isEqualTo(groups.find(size - 1));
        assertThat(groups.groups()).hasSize(1);
    }
}
