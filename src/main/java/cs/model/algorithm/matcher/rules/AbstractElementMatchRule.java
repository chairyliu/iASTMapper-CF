package cs.model.algorithm.matcher.rules;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;

/**
 * Base class of ElementMatchRule
 */
public abstract class AbstractElementMatchRule implements ElementMatchRule {

    protected static ElementMappings elementMappings;

    protected static boolean isParentMappingBasedPM(ElementSimMeasures measures, ElementMappings eleMappings) {
        double val = 0;
        ProgramElement srcEle = measures.getSrcEle();
        ProgramElement dstEle = measures.getDstEle();

        ProgramElement srcParentEle = srcEle.getParentElement();
        ProgramElement dstParentEle = dstEle.getParentElement();

        if (!elementMappings.isMapped(srcParentEle) && !elementMappings.isMapped(dstParentEle)){
            return false;
        }
        if (elementMappings.getDstForSrc(srcParentEle) == dstParentEle){
            val = 1;
            return true;
        }
        return false;
    }

    protected boolean isParentMapping(ProgramElement srcEle, ProgramElement dstEle) {
        ProgramElement srcParentEle = srcEle.getParentElement();
        ProgramElement dstParentEle = dstEle.getParentElement();
        return elementMappings.getDstForSrc(srcParentEle) == dstParentEle;
    }
}
