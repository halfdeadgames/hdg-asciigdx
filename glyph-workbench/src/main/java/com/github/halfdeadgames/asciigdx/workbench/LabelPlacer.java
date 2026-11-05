package com.github.halfdeadgames.asciigdx.workbench;

import java.util.Arrays;

/** Spreads labels along one axis so none overlap, moving each as little as possible and never changing their order. */
final class LabelPlacer {

    private LabelPlacer() { }

    /**
     * @param wanted preferred start of each label along the axis
     * @param sizes length of each label along the axis
     * @param min lowest allowed start
     * @param max highest allowed end
     * @param gap free space required between neighbours
     * @return start of each label, in input order; labels that cannot fit between the bounds are stacked from {@code min}
     */
    static int[] place(int[] wanted, int[] sizes, int min, int max, int gap) {
        int count = wanted.length;
        Integer[] order = indicesSortedByWanted(wanted);

        // A cluster is a run of touching labels that moves as one block; its position is the mean of what its members want.
        Clusters clusters = new Clusters(count);
        for (int rank = 0; rank < count; rank++) {
            int index = order[rank];
            clusters.firstOf[clusters.count] = rank;
            clusters.lengthOf[clusters.count] = 1;
            clusters.extentOf[clusters.count] = sizes[index];
            clusters.positionOf[clusters.count] = clampStart(wanted[index], sizes[index], min, max);
            clusters.count++;
            while (clusters.count > 1 && lastTwoClustersTouch(clusters, gap)) {
                mergeLastClusterIntoPrevious(clusters, order, wanted, sizes, gap, min, max);
                clusters.count--;
            }
        }
        return startsFromClusters(clusters, order, sizes, gap);
    }

    private static final class Clusters {
        final int[] firstOf;
        final int[] lengthOf;
        final int[] extentOf;
        final int[] positionOf;
        int count;

        Clusters(int capacity) {
            firstOf = new int[capacity];
            lengthOf = new int[capacity];
            extentOf = new int[capacity];
            positionOf = new int[capacity];
        }
    }

    private static Integer[] indicesSortedByWanted(int[] wanted) {
        Integer[] order = new Integer[wanted.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(wanted[a], wanted[b]));
        return order;
    }

    private static boolean lastTwoClustersTouch(Clusters clusters, int gap) {
        int last = clusters.count - 1;
        int previous = last - 1;
        return clusters.positionOf[previous] + clusters.extentOf[previous] + gap > clusters.positionOf[last];
    }

    private static void mergeLastClusterIntoPrevious(Clusters clusters, Integer[] order, int[] wanted, int[] sizes,
                                                     int gap, int min, int max) {
        int[] firstOf = clusters.firstOf;
        int[] lengthOf = clusters.lengthOf;
        int[] extentOf = clusters.extentOf;
        int[] positionOf = clusters.positionOf;
        int last = clusters.count - 1;
        int previous = last - 1;
        int mergedExtent = extentOf[previous] + gap + extentOf[last];
        long wantedSum = 0;
        int offset = 0;
        for (int rankInside = firstOf[previous]; rankInside < firstOf[last] + lengthOf[last]; rankInside++) {
            int member = order[rankInside];
            wantedSum += wanted[member] - offset;
            offset += sizes[member] + gap;
        }
        int memberCount = lengthOf[previous] + lengthOf[last];
        lengthOf[previous] = memberCount;
        extentOf[previous] = mergedExtent;
        positionOf[previous] = clampStart(Math.floorDiv(wantedSum, memberCount), mergedExtent, min, max);
    }

    private static int[] startsFromClusters(Clusters clusters, Integer[] order, int[] sizes, int gap) {
        int[] result = new int[order.length];
        for (int cluster = 0; cluster < clusters.count; cluster++) {
            int offset = 0;
            int firstRank = clusters.firstOf[cluster];
            for (int rank = firstRank; rank < firstRank + clusters.lengthOf[cluster]; rank++) {
                int member = order[rank];
                result[member] = clusters.positionOf[cluster] + offset;
                offset += sizes[member] + gap;
            }
        }
        return result;
    }

    private static int clampStart(long wantedStart, int extent, int min, int max) {
        return (int) Math.max(min, Math.min(wantedStart, max - extent));
    }
}
