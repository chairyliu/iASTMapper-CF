package cs.model.algorithm.matcher.measures.util;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;
import cs.model.utils.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mechanism: estimate the relevance of two program elements based on the mapped ancestors.
 *
 * For example, statements in mapped methods are more likely to be mapped.
 */
public class ElementAncestorMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        double val = Double.MAX_VALUE;
        Pair<ProgramElement, ProgramElement> elePair = calMappedAncestorElement(srcEle, dstEle);
        if (elePair != null) {
            List<ProgramElement> srcAncestors1 = elePair.first.getAncestors();
            List<ProgramElement> dstAncestors1 = elePair.second.getAncestors();
            List<ProgramElement> srcAncestors2 = srcEle.getAncestors();
            List<ProgramElement> dstAncestors2 = dstEle.getAncestors();
            val = srcAncestors2.size() + dstAncestors2.size() - srcAncestors1.size() - dstAncestors1.size();
        }
        return val;
    }

    @Override
    protected int compareMeasureVal(double val1, double val2) {
        return - Double.compare(val1, val2);
    }

    private Pair<ProgramElement, ProgramElement> calMappedAncestorElement(ProgramElement srcEle,
                                                                          ProgramElement dstEle) {
        List<ProgramElement> srcAncestors = srcEle.getAncestors();
        List<ProgramElement> dstAncestors = dstEle.getAncestors();

        Set<ProgramElement> dstAncestorSet = new HashSet<>(dstAncestors);
        int i = srcAncestors.size() - 1;
        while (i >= 0) {
            ProgramElement tmpSrcEle = srcAncestors.get(i);
            if (elementMappings.isMapped(tmpSrcEle)) {
                ProgramElement tmpDstEle = elementMappings.getDstForSrc(tmpSrcEle);
                if (dstAncestorSet.contains(tmpDstEle))
                    return new Pair<>(tmpSrcEle, tmpDstEle);
            }
            i--;
        }
        return null;
    }
}
