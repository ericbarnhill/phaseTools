package com.ericbarnhill.phaseTools;

public class Differences3D {

    public static int[][] getDifferences() {

        int[][] differences3D = new int[][] { 
            {-1,-1,-1},
            {1,1,1},
            {0,-1,-1},
            {0,1,1},
            {1,-1,-1},
            {1,1,1},
            {-1,0,-1},
            {1,0,1},
            {0,0,-1},
            {0,0,1},
            {1,0,-1},
            {-1,0,1},
            {-1,1,-1},
            {1,-1,1},
            {0,1,-1},
            {0,-1,1},
            {1,1,-1},
            {-1,-1,1},
            {-1,-1,0},
            {1,1,0},
            {0,-1,0},
            {0,1,0},
            {1,-1,0},
            {-1,1,0},
            {-1,0,0},
            {1,0,0} 
        };
        return differences3D;

    }

}
