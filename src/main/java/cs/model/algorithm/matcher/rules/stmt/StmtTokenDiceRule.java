package cs.model.algorithm.matcher.rules.stmt;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.measures.SimMeasureNames;
import cs.model.algorithm.matcher.rules.AbstractElementMatchRule;
import cs.model.algorithm.matcher.rules.ElementMatchRule;

public class StmtTokenDiceRule extends AbstractElementMatchRule implements ElementMatchRule {
    public StmtTokenDiceRule(){
        super();
    }
    @Override
    public boolean determineCanBeMapped(ElementSimMeasures measures, ElementMappings eleMappings) {
        ProgramElement srcElement = measures.getSrcEle();
        if (typeChecker.isPackageDec(srcElement.getITreeNode()))
            return true;

        double dice = measures.getSimMeasure(SimMeasureNames.IMTR, eleMappings).getValue();
        if (typeChecker.isImportDec(srcElement.getITreeNode()))
            return dice >= stmtDiceThreshold0;

        if (isParentMappingBasedPM(measures, eleMappings))
            return dice >= stmtDiceThreshold1;
        else
            return dice >= stmtDiceThreshold2;
    }
}
