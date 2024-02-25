package cs.model.algorithm.matcher.rules.stmt;

import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.measures.SimMeasureNames;
import cs.model.algorithm.matcher.rules.AbstractElementMatchRule;
import cs.model.algorithm.matcher.rules.ElementMatchRule;

public class SameMethodBodyMatchRule extends AbstractElementMatchRule implements ElementMatchRule {
    public SameMethodBodyMatchRule(){
        super();
    }
    @Override
    public boolean determineCanBeMapped(ElementSimMeasures measures, ElementMappings eleMappings) {
        return measures.getSimMeasure(SimMeasureNames.SAME_METHOD_BODY, eleMappings).getValue() == 1.0;
    }
}
