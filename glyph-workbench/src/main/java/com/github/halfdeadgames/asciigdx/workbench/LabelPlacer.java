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
        int[] firstOf = new int[count];
        int[] lengthOf = new int[count];
        int[] extentOf = new int[count];
        int[] positionOf = new int[count];
        int clusters = 0;
        for (int rank = 0; rank < count; rank++) {
            int index = order[rank];
            firstOf[clusters] = rank;
            lengthOf[clusters] = 1;
            extentOf[clusters] = sizes[index];
            positionOf[clusters] = clampStart(wanted[index], sizes[index], min, max);
            clusters++;
            while (clusters > 1 && lastTwoClustersTouch(extentOf, positionOf, clusters, gap)) {
                mergeLastClusterIntoPrevious(order, wanted, sizes, firstOf, lengthOf, extentOf, positionOf,
                        clusters - 1, gap, min, max);
                clusters--;
            }
        }
        return startsFromClusters(order, sizes, firstOf, lengthOf, positionOf, clusters, gap);
    }

    private static Integer[] indicesSortedByWanted(int[] wanted) {
        Integer[] order = new Integer[wanted.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(wanted[a], wanted[b]));
        return order;
    }

    private static boolean lastTwoClustersTouch(int[] extentOf, int[] positionOf, int clusters, int gap) {
        int last = clusters - 1;
        int previous = last - 1;
        return positionOf[previous] + extentOf[previous] + gap > positionOf[last];
    }

    private static void mergeLastClusterIntoPrevious(Integer[] order, int[] wanted, int[] sizes, int[] firstOf,
                                                     int[] lengthOf, int[] extentOf, int[] positionOf,
                                                     int last, int gap, int min, int max) {
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

    private static int[] startsFromClusters(Integer[] order, int[] sizes, int[] firstOf, int[] lengthOf,
                                            int[] positionOf, int clusters, int gap) {
        int[] result = new int[order.length];
        for (int cluster = 0; cluster < clusters; cluster++) {
            int offset = 0;
            for (int rank = firstOf[cluster]; rank < firstOf[cluster] + lengthOf[cluster]; rank++) {
                int member = order[rank];
                result[member] = positionOf[cluster] + offset;
                offset += sizes[member] + gap;
            }
        }
        return result;
    }

    private static int clampStart(long wantedStart, int extent, int min, int max) {
        return (int) Math.max(min, Math.min(wantedStart, max - extent));
    }
}
