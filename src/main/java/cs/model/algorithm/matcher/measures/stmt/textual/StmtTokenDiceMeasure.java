package cs.model.algorithm.matcher.measures.stmt.textual;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

/**
 * Mechanism: statements sharing more identical tokens are more likely to be mapped.
 */
public class StmtTokenDiceMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        StmtElement srcStmtEle = (StmtElement) srcEle;
        StmtElement dstStmtEle = (StmtElement) dstEle;
        int srcTokenNum = srcStmtEle.getNameAndLiteralNum();
        int dstTokenNum = dstStmtEle.getNameAndLiteralNum();
//        System.out.println("Src dice is " + srcStmtEle + " " + srcTokenNum + "   dst is " + dstStmtEle + " " + dstTokenNum);
        if (srcTokenNum == 0 || dstTokenNum == 0)
            return 0;
//        System.out.println("Src dice is " + srcStmtEle + " " + srcTokenNum + "   dst is " + dstStmtEle + " " + dstTokenNum);
        SimMeasure mappedOrIdenticalStmt = new StmtIdenticalOrMappedTokenMeasure();
        mappedOrIdenticalStmt.setElementMappings(elementMappings);
        mappedOrIdenticalStmt.calSimMeasure(srcEle, dstEle);
        double val = mappedOrIdenticalStmt.getValue();
        return 2 * val / (srcTokenNum + dstTokenNum);
    }
}
