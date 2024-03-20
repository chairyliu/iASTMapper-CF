package cs.model.algorithm.matcher.rules.token;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.measures.SimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasureNames;
import cs.model.algorithm.matcher.rules.AbstractElementMatchRule;
import cs.model.algorithm.matcher.rules.ElementMatchRule;

/**
 * Mapping rule for token
 *
 * Tokens in mapped statements are likely to be mapped.
 */
public class TokenSameStmtRule extends AbstractElementMatchRule implements ElementMatchRule {
    public  TokenSameStmtRule(){
        super();
    }

    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        if (srcEle.getStringValue().equals(dstEle.getStringValue()))
            return 1;
        if (elementMappings.isTokenRenamed(srcEle, dstEle))
            return 1;
        return 0;
    }

    @Override
    public boolean determineCanBeMapped(ElementSimMeasures measures, ElementMappings eleMappings) {
        SimMeasure measure1 = measures.getSimMeasure(SimMeasureNames.T_MSIS, eleMappings);
        this.elementMappings = eleMappings;
        ProgramElement srcEle = measures.getSrcEle();
        ProgramElement dstEle = measures.getDstEle();
        if (measure1.getValue() == 1.0) {
            // if two tokens have the same value or the two tokens are renamed
//            SimMeasure measure2 = measures.getSimMeasure(SimMeasureNames.SAME_VALUE_RENAME, eleMappings);
            if (calMeasureValue(srcEle, dstEle) == 1.0)
                return true;
        }
        return false;
    }
}
