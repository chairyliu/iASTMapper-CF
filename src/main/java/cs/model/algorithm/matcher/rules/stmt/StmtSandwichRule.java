package cs.model.algorithm.matcher.rules.stmt;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.measures.SimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasureNames;
import cs.model.algorithm.matcher.rules.AbstractElementMatchRule;
import cs.model.algorithm.matcher.rules.ElementMatchRule;


/**
 * Mapping rule for statement.
 *
 * Statements with surrounding statements mapped can be mapped.
 */
public class StmtSandwichRule extends AbstractElementMatchRule implements ElementMatchRule {
    public  StmtSandwichRule(){
        super();
    }
    @Override
    public boolean determineCanBeMapped(ElementSimMeasures measures, ElementMappings eleMappings) {
        ProgramElement srcElement = measures.getSrcEle();
        if (typeChecker.isBlock(srcElement.getITreeNode()))
            return false;
        SimMeasure measure = measures.getSimMeasure(SimMeasureNames.STMT_SANDWICH, eleMappings);
        return measure.getValue() == 1.0;
    }
}


