package cs.model.algorithm.utils;

import java.util.LinkedList;
import java.util.List;

/**
 * The longest common subsequence class.
 *
 * Given a list of objects, match them using the given equal function.
 * @param <T> The type of the objects
 */
public abstract class LongestCommonSubsequence<T> {
    private List<T> sequence1;
    private List<T> sequence2;
    private int[][] lengthMatrix;
    private int[][] equalMatrix;
    private int lcsLength;

    private int max1;
    private int max2;

    public LongestCommonSubsequence(List<T> sequence1, List<T> sequence2) {
        this.sequence1 = sequence1;
        this.sequence2 = sequence2;
        this.lengthMatrix = new int[sequence1.size() + 1][sequence2.size() + 1];
        this.equalMatrix = new int[sequence1.size() + 1][sequence2.size() + 1];
        calSequenceLength();
    }

    /**
     * Determine if t1 and t2 can be matched.
     * @param t1 object 1
     * @param t2 object 2
     * @return true if t1 and t2 can be matched
     */
    public abstract boolean isEqual(T t1, T t2);

    private void calSequenceLength(){
        lcsLength = 0;
        max1 = 0;
        max2 = 0;
        for (int i = 1; i <= sequence1.size(); i ++){
            for (int j = 1; j <= sequence2.size(); j++ ){
                if (isEqual(sequence1.get(i - 1), sequence2.get(j - 1))) {
                    lengthMatrix[i][j] = lengthMatrix[i - 1][j - 1] + 1;
                    equalMatrix[i][j] = 1;

                    if (i > max1)
                        max1 = i;
                    if (j > max2)
                        max2 = j;

                } else {
                    lengthMatrix[i][j] = Integer.max(lengthMatrix[i - 1][j], lengthMatrix[i][j - 1]);
                    equalMatrix[i][j] = 0;
                }

                if (lengthMatrix[i][j] > lcsLength)
                    lcsLength = lengthMatrix[i][j];
            }
        }
    }

    /**
     * Calculate a default longest common subsequence.
     * The lcs is represented as a list of index pairs.
     * Each index pair represents a pair of element
     * in the two lists that are matched.
     *
     * @return a list of index pairs.
     */
    public List<int[]> extractIdxes(){
        List<int[]> indexes = new LinkedList<>();
        for (int x = sequence1.size(), y = sequence2.size(); x != 0 && y != 0; ){
            if (lengthMatrix[x][y] == lengthMatrix[x-1][y]) x--;
            else if (lengthMatrix[x][y] == lengthMatrix[x][y-1]) y--;
            else {
                indexes.add(0, new int[] {x - 1, y - 1});
                x --;
                y --;
            }
        }
        return indexes;
    }
}
