package cs.model.algorithm.matcher.measures.token;

import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.matchers.searchers.CandidateSetsAndMaps;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

import java.util.HashSet;
import java.util.Set;

/*
 * Mechanism: two tokens from mapped inner statements are likely to be mapped.
 */
public class InnerStmtMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        double val = 0;
        InnerStmtElement srcInnerStmtEle = ((TokenElement) srcEle).getNearestMultiTokenInnerStmtElement();
        InnerStmtElement dstInnerStmtEle = ((TokenElement) dstEle).getNearestMultiTokenInnerStmtElement();
        if (elementMappings.getDstForSrc(srcInnerStmtEle) == dstInnerStmtEle)
            val = 1;
        return val;
    }

    @Override
    public Set<ProgramElement> filterBadDstCandidateElements(ProgramElement srcEle, Set<ProgramElement> dstCandidates,
                                                             CandidateSetsAndMaps candidateSetsAndMaps) {
        if (!srcEle.isFromSrc())
            return null;

        Set<ProgramElement> ret = new HashSet<>();
        if (dstCandidates.size() == 0)
            return ret;

        ProgramElement srcStmt = ((TokenElement) srcEle).getStmtElement();
        if (elementMappings.isMapped(srcStmt)) {
            ProgramElement dstStmt = elementMappings.getMappedElement(srcStmt);
            ret.addAll(dstStmt.getTokenElements());
            ret.retainAll(dstCandidates);
        }
        return ret;
    }
}