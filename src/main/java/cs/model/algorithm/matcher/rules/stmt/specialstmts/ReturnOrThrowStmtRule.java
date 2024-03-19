package cs.model.algorithm.matcher.rules.stmt.specialstmts;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.rules.AbstractElementMatchRule;
import cs.model.algorithm.matcher.rules.ElementMatchRule;

/**
 * Mapping rule for return statements.
 *
 * Return statements can be mapped if they share the same method declaration.
 */
public class ReturnOrThrowStmtRule extends AbstractElementMatchRule implements ElementMatchRule {
    public  ReturnOrThrowStmtRule(){
        super();
    }

    private boolean isReturnOrThrowStatement(ProgramElement element) {
        return typeChecker.isReturnStatement(element.getITreeNode()) ||
                typeChecker.isThrowStatement(element.getITreeNode());
    }

    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        if (!isReturnOrThrowStatement(srcEle))
            return 0;

        ProgramElement srcMethod = ((StmtElement) srcEle).getMethodOfElement();
        ProgramElement dstMethod = ((StmtElement) dstEle).getMethodOfElement();
        if (elementMappings.getDstForSrc(srcMethod) == dstMethod)
            return 1.0;
        return 0;
    }
    @Override
    public boolean determineCanBeMapped(ElementSimMeasures measures, ElementMappings eleMappings) {
        this.elementMappings = eleMappings;
        ProgramElement srcEle = measures.getSrcEle();
        ProgramElement dstEle = measures.getDstEle();
        return calMeasureValue(srcEle, dstEle) == 1.0;
    }
}


