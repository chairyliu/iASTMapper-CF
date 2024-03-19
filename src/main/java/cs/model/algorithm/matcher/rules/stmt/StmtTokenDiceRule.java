package cs.model.algorithm.matcher.rules.stmt;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
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
//            return dice == 1;

        if (isParentMappingBasedPM(measures, eleMappings))
            return dice >= stmtDiceThreshold1;
        else
            return dice >= stmtDiceThreshold2;
    }

    private boolean isMethodMapping(ElementSimMeasures measures, ElementMappings eleMappings) {
        ProgramElement srcEle = measures.getSrcEle();
        ProgramElement dstEle = measures.getDstEle();
        ProgramElement srcMethodEle = ((StmtElement) srcEle).getMethodOfElement();
        ProgramElement dstMethodEle = ((StmtElement) dstEle).getMethodOfElement();
        if (srcMethodEle == null || dstMethodEle == null)
            return false;
        return eleMappings.getMappedElement(srcMethodEle) == dstMethodEle;
    }
}
