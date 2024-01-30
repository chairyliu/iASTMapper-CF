package cs.model.algorithm.matcher.fastmatchers;

import cs.model.algorithm.element.ProgramElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Store token mapping information for each statement
 */
public class TokenMapStatistics {
    private Map<ProgramElement, Integer> srcStmtMappedTokenNumMap;
    private Map<ProgramElement, Integer> dstStmtMappedTokenNumMap;

    public TokenMapStatistics(){
        srcStmtMappedTokenNumMap = new HashMap<>();
        dstStmtMappedTokenNumMap = new HashMap<>();
    }

    /**
     * Record that all tokens of the statement are mapped.
     * @param stmt the statement
     */
    public void recordAllTokenForStmt(ProgramElement stmt){
        if (stmt.isFromSrc())
            srcStmtMappedTokenNumMap.put(stmt, stmt.getTokenElements().size());
        else
            dstStmtMappedTokenNumMap.put(stmt, stmt.getTokenElements().size());
    }

    /**
     * Whether all tokens of a statement are mapped.
     * @param stmt statement to analyze
     * @return true if all tokens of the statement are mapped.
     */
    public boolean isAllTokenMapped(ProgramElement stmt){
        if (stmt.isFromSrc())
            return srcStmtMappedTokenNumMap.containsKey(stmt) &&
                    srcStmtMappedTokenNumMap.get(stmt) == stmt.getTokenElements().size();
        else
            return dstStmtMappedTokenNumMap.containsKey(stmt) &&
                    dstStmtMappedTokenNumMap.get(stmt) == stmt.getTokenElements().size();
    }
}
