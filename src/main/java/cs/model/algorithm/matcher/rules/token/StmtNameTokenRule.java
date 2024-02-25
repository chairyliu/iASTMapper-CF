package cs.model.algorithm.matcher.rules.token;

import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.measures.SimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasureNames;
import cs.model.algorithm.matcher.rules.AbstractElementMatchRule;
import cs.model.algorithm.matcher.rules.ElementMatchRule;

public class StmtNameTokenRule extends AbstractElementMatchRule implements ElementMatchRule {
    public StmtNameTokenRule(){
        super();
    }
    @Override
    public boolean determineCanBeMapped(ElementSimMeasures measures, ElementMappings eleMappings) {
        SimMeasure measure = measures.getSimMeasure(SimMeasureNames.STMT_NAME_TOKEN, eleMappings);
        return measure.getValue() == 1.0;
    }
}
