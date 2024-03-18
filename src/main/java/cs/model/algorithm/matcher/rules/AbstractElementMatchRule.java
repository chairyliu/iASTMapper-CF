package cs.model.algorithm.matcher.rules;

import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.measures.SimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasureNames;

/**
 * Base class of ElementMatchRule
 */
public abstract class AbstractElementMatchRule implements ElementMatchRule {
    protected static boolean isWithSameName(ElementSimMeasures measures, ElementMappings eleMappings) {
        SimMeasure nameSameMeasure = measures.getSimMeasure(SimMeasureNames.STMT_NAME, eleMappings);
        return nameSameMeasure.getValue() == 1.0;
    }

    protected static boolean isWithSameSignature(ElementSimMeasures measures, ElementMappings eleMappings) {
        SimMeasure signatureMeasures = measures.getSimMeasure(SimMeasureNames.MS, eleMappings);
        return signatureMeasures.getValue() == 1.0;
    }

    protected static boolean isParentMapping(ElementSimMeasures measures, ElementMappings eleMappings) {
        SimMeasure pm = measures.getSimMeasure(SimMeasureNames.PM, eleMappings);
        return pm.getValue() == 1.0;
    }

    // If a token is moved to another stmt and its neighbor token is mapped,
    // this token can also be mapped.
    protected static boolean moveWithNeighborToken(ElementSimMeasures measures, ElementMappings eleMappings){
        SimMeasure measure = measures.getSimMeasure(SimMeasureNames.TOKEN_NEIGHBOR, eleMappings);
        return measure.getValue() == 1.0;
    }
}
