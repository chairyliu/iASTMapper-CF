package cs.model.algorithm.matcher.measures.stmt;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

import java.util.Set;

/**
 * Mechanism: mapped statements should have as many common descendant stmts as possible.
 */
public class StmtDescendantMappingMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        double val = 0;

        // For block element and their parent element are not mapped, we do not calculate DM
        if (typeChecker.isBlock(srcEle.getITreeNode()) && !isParentMapping(srcEle, dstEle))
            return val;

        // process cases where no descendant statement is found
        if (srcEle.getNearestDescendantStmts().size() == 0 || dstEle.getNearestDescendantStmts().size() == 0)
            return val;

        Set<ProgramElement> srcSubStmts = ((StmtElement) srcEle).getAllNonBlockDescendantStmts();
        Set<ProgramElement> dstSubStmts = ((StmtElement) dstEle).getAllNonBlockDescendantStmts();
        for (ProgramElement srcStmt: srcSubStmts){
            if (elementMappings.isMapped(srcStmt)) {
                ProgramElement dstStmt = elementMappings.getDstForSrc(srcStmt);
                if (dstSubStmts.contains(dstStmt)) {
                    val += 1;
                }
            }
        }
        if (val > 0)
            val = val * 2 / (srcSubStmts.size() + dstSubStmts.size());
        return val;
    }
}
