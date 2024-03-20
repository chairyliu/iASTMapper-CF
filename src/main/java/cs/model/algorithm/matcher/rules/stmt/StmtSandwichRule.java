package cs.model.algorithm.matcher.rules.stmt;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.rules.AbstractElementMatchRule;
import cs.model.algorithm.matcher.rules.ElementMatchRule;

import java.util.List;


/**
 * Mapping rule for statement.
 *
 * Statements with surrounding statements mapped can be mapped.
 */
public class StmtSandwichRule extends AbstractElementMatchRule implements ElementMatchRule {
    public  StmtSandwichRule(){
        super();
    }

    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        if (typeChecker.isBlock(srcEle.getITreeNode())){
            return 0;
        }

        if (srcEle.isDeclaration())
            return 0;

        List<StmtElement> srcSiblingElements = srcEle.getParentElement().getNearestDescendantStmts();
        List<StmtElement> dstSiblingElements = dstEle.getParentElement().getNearestDescendantStmts();

        // We consider this measure when the stmt have common parent nodes.
        if (!isParentMapping(srcEle, dstEle))
            return 0;

        if (srcSiblingElements.size() == 1 && dstSiblingElements.size() == 1) {
            return 1;
        }
        boolean leftMapped = isLeftStmtMapped(srcEle, dstEle);
        boolean rightMapped = isRightStmtMapped(srcEle, dstEle);
//        System.out.println("Brother " + srcEle + " | " + dstEle + " " + leftMapped + " " + rightMapped);
        if (leftMapped && rightMapped)
            return 1;
        return 0;
    }

    protected boolean isLeftStmtMapped(ProgramElement srcElement, ProgramElement dstElement) {
        List<StmtElement> srcSiblingElements = srcElement.getParentElement().getNearestDescendantStmts();
        List<StmtElement> dstSiblingElements = dstElement.getParentElement().getNearestDescendantStmts();
        boolean leftMapped = false;
        if (srcElement.getChildIdx() == 0 && dstElement.getChildIdx() == 0)
            leftMapped = true;
        else if (srcElement.getChildIdx() > 0 && dstElement.getChildIdx() > 0) {
            ProgramElement srcEle1 = srcSiblingElements.get(srcElement.getChildIdx() - 1);
            ProgramElement dstEle1 = dstSiblingElements.get(dstElement.getChildIdx() - 1);

//            System.out.println("Left Src is " + srcElement + " " + srcEle1 + " Dst is " + dstElement + " " + dstEle1);
            if (elementMappings.getDstForSrc(srcEle1) == dstEle1)
                leftMapped = true;
        }
        return leftMapped;
    }

    protected boolean isRightStmtMapped(ProgramElement srcElement, ProgramElement dstElement) {
        List<StmtElement> srcSiblingElements = srcElement.getParentElement().getNearestDescendantStmts();
        List<StmtElement> dstSiblingElements = dstElement.getParentElement().getNearestDescendantStmts();
        boolean rightMapped = false;
        int srcSiblingSize = srcSiblingElements.size();
        int dstSiblingSize = dstSiblingElements.size();
        if (srcElement.getChildIdx() == srcSiblingSize - 1 && dstElement.getChildIdx() == dstSiblingSize - 1)
            rightMapped = true;
        else if (srcElement.getChildIdx() < srcSiblingSize - 1 && dstElement.getChildIdx() < dstSiblingSize - 1){
            ProgramElement srcEle2 = srcSiblingElements.get(srcElement.getChildIdx() + 1);
            ProgramElement dstEle2 = dstSiblingElements.get(dstElement.getChildIdx() + 1);
//            System.out.println("Right Src is " + srcElement + " " + srcEle2 + " Dst is " + dstElement + " " + dstEle2);
            if (elementMappings.getDstForSrc(srcEle2) == dstEle2)
                rightMapped = true;
        }
        return rightMapped;
    }

    @Override
    public boolean determineCanBeMapped(ElementSimMeasures measures, ElementMappings eleMappings) {
        this.elementMappings = eleMappings;
        ProgramElement srcElement = measures.getSrcEle();
        ProgramElement dstElement = measures.getDstEle();
        return calMeasureValue(srcElement, dstElement) == 1.0;
    }
}


