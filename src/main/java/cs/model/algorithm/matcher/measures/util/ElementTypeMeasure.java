package cs.model.algorithm.matcher.measures.util;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

/**
 * Mechanism: Elements with the same type can be mapped.
 *
 * Furthermore, for special tokens, they can only be mapped to a set of tokens.
 * For instance, public is not likely to be mapped to static.
 */
public class ElementTypeMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        return srcEle.getElementType().equals(dstEle.getElementType()) ? 1 : 0;
    }
}
