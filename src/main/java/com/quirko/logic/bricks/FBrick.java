package com.quirko.logic.bricks;

import com.quirko.logic.MatrixOperations;

import java.util.ArrayList;
import java.util.List;

public final class FBrick implements Brick {

    private final List<int[][]> brickMatrix = new ArrayList<>();

    public FBrick() {
        brickMatrix.add(new int[][]{
                {1, 2, 3, 4},
                {2, 0, 0, 0},
                {3, 5, 6, 0},
                {4, 0, 0, 0}
        });
        brickMatrix.add(new int[][]{
                {5, 0, 0, 0},
                {4, 0, 7, 0},
                {3, 0, 6, 0},
                {2, 3, 4, 5}
        });
        brickMatrix.add(new int[][]{
                {0, 0, 0, 6},
                {0, 1, 7, 5},
                {0, 0, 0, 4},
                {6, 5, 4, 3}
        });
        brickMatrix.add(new int[][]{
                {7, 6, 5, 4},
                {0, 1, 0, 5},
                {0, 2, 0, 6},
                {0, 0, 0, 7}
        });
    }

    @Override
    public List<int[][]> getShapeMatrix() {
        return MatrixOperations.deepCopyList(brickMatrix);
    }
}
