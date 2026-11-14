package com.github.halfdeadgames.asciigdx.shape;

import java.util.ArrayList;
import java.util.List;

/** Test visitor that records every visit. */
final class RecordedVisits implements ShapeCellVisitor {

    record Visit(int column, int row, float distance, float distanceFraction) {
    }

    final List<Visit> visits = new ArrayList<>();

    static RecordedVisits of(CellShape shape) {
        final RecordedVisits recorded = new RecordedVisits();
        shape.forEachCell(recorded);
        return recorded;
    }

    @Override
    public void visit(int column, int row, float distance, float distanceFraction) {
        visits.add(new Visit(column, row, distance, distanceFraction));
    }

    int count() {
        return visits.size();
    }

    Visit get(int index) {
        return visits.get(index);
    }

    Visit at(int column, int row) {
        for (final Visit visit : visits) {
            if (visit.column() == column && visit.row() == row) {
                return visit;
            }
        }
        return null;
    }

    boolean contains(int column, int row) {
        return at(column, row) != null;
    }

    int countInRow(int row) {
        int count = 0;
        for (final Visit visit : visits) {
            if (visit.row() == row) {
                count++;
            }
        }
        return count;
    }

    int minColumnInRow(int row) {
        int min = Integer.MAX_VALUE;
        for (final Visit visit : visits) {
            if (visit.row() == row) {
                min = Math.min(min, visit.column());
            }
        }
        return min;
    }

    int maxColumnInRow(int row) {
        int max = Integer.MIN_VALUE;
        for (final Visit visit : visits) {
            if (visit.row() == row) {
                max = Math.max(max, visit.column());
            }
        }
        return max;
    }

    boolean hasNoDuplicates() {
        for (int i = 0; i < visits.size(); i++) {
            for (int j = i + 1; j < visits.size(); j++) {
                if (visits.get(i).column() == visits.get(j).column()
                        && visits.get(i).row() == visits.get(j).row()) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean isInRowMajorOrder() {
        for (int i = 1; i < visits.size(); i++) {
            final Visit previous = visits.get(i - 1);
            final Visit current = visits.get(i);
            final boolean laterRow = current.row() > previous.row();
            final boolean sameRowFurtherRight = current.row() == previous.row()
                    && current.column() > previous.column();
            if (!laterRow && !sameRowFurtherRight) {
                return false;
            }
        }
        return true;
    }

    float largestFraction() {
        float largest = 0f;
        for (final Visit visit : visits) {
            largest = Math.max(largest, visit.distanceFraction());
        }
        return largest;
    }

    boolean allFractionsInUnitRange() {
        for (final Visit visit : visits) {
            if (Float.isNaN(visit.distanceFraction()) || visit.distanceFraction() < 0f
                    || visit.distanceFraction() > 1f) {
                return false;
            }
        }
        return true;
    }
}
