package cs.model.algorithm.matcher.measures.innerstmt;

import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

import java.util.List;


/**
 * Mechanism: for two method invocations, assignments or other elements, if their names
 * are mapped and they can can be mapped.
 */
public class InnerStmtSameStmtMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        double val = 0;
        int count = 0;
        ProgramElement srcStmtEle = srcEle.getStmtElement();
        ProgramElement dstStmtEle = dstEle.getStmtElement();
        List<InnerStmtElement> srcInnerStmtEle = srcEle.getInnerStmtElements();
        List<InnerStmtElement> dstInnerStmtEle = dstEle.getInnerStmtElements();
        for (InnerStmtElement srcInnerStmt : srcInnerStmtEle){
            if (elementMappings.isMapped(srcInnerStmt)){
                if (dstInnerStmtEle.contains(elementMappings.getMappedElement(srcInnerStmt))){
                    count += 1;
                } else {
                    break;
                }
            }
        }
        if (elementMappings.getDstForSrc(srcStmtEle) == dstStmtEle || count == srcInnerStmtEle.size())
            val = 1;
        return val;
    }
}
