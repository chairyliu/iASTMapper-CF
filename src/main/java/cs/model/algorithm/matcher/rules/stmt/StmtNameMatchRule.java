package cs.model.algorithm.matcher.rules.stmt;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.rules.AbstractElementMatchRule;
import cs.model.algorithm.matcher.rules.ElementMatchRule;

/**
 * Mapping rule for statements with name.
 *
 * Statements with the same name
 */
public class StmtNameMatchRule extends AbstractElementMatchRule implements ElementMatchRule {
    public  StmtNameMatchRule(){
        super();
    }

    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        if (srcEle.getName() == null || dstEle.getName() == null)
            return 0;
//        System.out.println("Src and name " + srcEle + " " + srcEle.getName() + " Dst and name " + dstEle + " " + dstEle.getName());
        boolean equalName = srcEle.getName().equals(dstEle.getName());
        return equalName || isWithRename(srcEle, dstEle) ? 1 : 0;
    }

    protected boolean isWithRename(ProgramElement srcElement, ProgramElement dstElement) {
        TokenElement srcNameToken = ((StmtElement) srcElement).getNameToken();
        TokenElement dstNameToken = ((StmtElement) dstElement).getNameToken();
//        System.out.println("Name Token is " + srcNameToken + " | " + dstNameToken);
        if (srcNameToken != null && dstNameToken != null) {
            return elementMappings.isTokenRenamed(srcNameToken, dstNameToken);
        }
        return false;
    }

    @Override
    public boolean determineCanBeMapped(ElementSimMeasures measures, ElementMappings eleMappings) {
        this.elementMappings = eleMappings;
        ProgramElement srcEle = measures.getSrcEle();
        ProgramElement dstEle = measures.getDstEle();
        return calMeasureValue(srcEle, dstEle) == 1.0;
    }
}
