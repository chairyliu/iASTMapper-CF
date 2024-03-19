package cs.model.algorithm.matcher.rules;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.measures.SimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasureNames;

/**
 * Base class of ElementMatchRule
 */
public abstract class AbstractElementMatchRule implements ElementMatchRule {

    protected ElementMappings elementMappings;

    protected static boolean isWithSameName(ElementSimMeasures measures, ElementMappings eleMappings) {
        SimMeasure nameSameMeasure = measures.getSimMeasure(SimMeasureNames.STMT_NAME, eleMappings);
        return nameSameMeasure.getValue() == 1.0;
    }

    protected static boolean isParentMappingBasedPM(ElementSimMeasures measures, ElementMappings eleMappings) {
        SimMeasure pm = measures.getSimMeasure(SimMeasureNames.PM, eleMappings);
        return pm.getValue() == 1.0;
    }

    protected boolean isParentMapping(ProgramElement srcEle, ProgramElement dstEle) {
        ProgramElement srcParentEle = srcEle.getParentElement();
        ProgramElement dstParentEle = dstEle.getParentElement();
        return elementMappings.getDstForSrc(srcParentEle) == dstParentEle;
    }
}
