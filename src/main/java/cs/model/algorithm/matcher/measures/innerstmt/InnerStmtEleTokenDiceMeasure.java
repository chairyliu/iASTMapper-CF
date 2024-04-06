package cs.model.algorithm.matcher.measures.innerstmt;

import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

import java.util.HashSet;
import java.util.Set;

/**
 * Mechanism: inner stmt elements with many tokens mapped are likely to be mapped.
 */
public class InnerStmtEleTokenDiceMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        InnerStmtElement srcElement = (InnerStmtElement) srcEle;
        InnerStmtElement dstElement = (InnerStmtElement) dstEle;
        double mappingValue = getDiceForMappedTokens(elementMappings, srcElement, dstElement);
        double equalValue = getDiceForEqualTokens(elementMappings, srcElement, dstElement);
        double val = mappingValue + equalValue;
        return 2 * val / (srcElement.getTokenElements().size() + dstElement.getTokenElements().size());
//        return InnerStmtElement.getDiceForMappedTokens(elementMappings, srcElement, dstElement);
    }

    public static double getDiceForMappedTokens(ElementMappings mappings, InnerStmtElement srcEle,
                                                InnerStmtElement dstEle) {
        Set<ProgramElement> srcTokens = new HashSet<>(srcEle.getTokenElements());
        Set<ProgramElement> dstTokens = new HashSet<>(dstEle.getTokenElements());
        double value = 0;
        for (ProgramElement srcTokenEle: srcTokens) {
            if (mappings.isMapped(srcTokenEle)) {
                if (dstTokens.contains(mappings.getMappedElement(srcTokenEle)))
                    value += 1.0;
            }
        }
        return value;
    }

    public static double getDiceForEqualTokens(ElementMappings mappings, InnerStmtElement srcEle,
                                               InnerStmtElement dstEle) {
        Set<ProgramElement> srcTokens = new HashSet<>(srcEle.getTokenElements());
        Set<ProgramElement> dstTokens = new HashSet<>(dstEle.getTokenElements());
        return Math.min(srcTokens.size(), dstTokens.size());
    }
}
