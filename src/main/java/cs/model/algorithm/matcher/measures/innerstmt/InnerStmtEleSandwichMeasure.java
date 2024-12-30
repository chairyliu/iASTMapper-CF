package cs.model.algorithm.matcher.measures.innerstmt;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

public class InnerStmtEleSandwichMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        double val = 0;
        ProgramElement srcParentEle = srcEle.getParentElement();
        ProgramElement dstParentEle = dstEle.getParentElement();

        if (elementMappings.getMappedElement(srcParentEle) != dstParentEle) {
            return 0;
        }

        if (srcParentEle.getInnerStmtElements().size() == 1 && dstParentEle.getInnerStmtElements().size() == 1) {
            return 1;
        }

        if (isLeftInnerEleMapped(srcEle, dstEle) && isRightInnerELeMapped(srcEle, dstEle))
            val = 2;

        if (isLeftInnerEleMapped(srcEle, dstEle) || isRightInnerELeMapped(srcEle, dstEle))
            val = 1;

        if (!isLeftInnerEleMapped(srcEle, dstEle) && !isRightInnerELeMapped(srcEle, dstEle))
            val = 0;

        return val;
    }

    private boolean isLeftInnerEleMapped(ProgramElement srcEle, ProgramElement dstEle){
        ProgramElement leftSrcInnerEle = srcEle.getLeftSibling();
        ProgramElement leftDstInnerEle = dstEle.getLeftSibling();
        boolean leftMapped = false;
        if (leftSrcInnerEle == null && leftDstInnerEle == null)
            leftMapped = true;
        else if (leftSrcInnerEle != null && leftDstInnerEle != null)
            leftMapped = elementMappings.getMappedElement(leftSrcInnerEle) == leftDstInnerEle;

        return leftMapped;
    }

    private boolean isRightInnerELeMapped(ProgramElement srcEle, ProgramElement dstEle){
        ProgramElement rightSrcInnerEle = srcEle.getRightSibling();
        ProgramElement rightDstInnerEle = dstEle.getRightSibling();
        boolean rightMapped = false;
        if (rightSrcInnerEle == null && rightDstInnerEle == null)
            rightMapped = true;
        else if (rightSrcInnerEle != null && rightDstInnerEle != null)
            rightMapped = elementMappings.getMappedElement(rightSrcInnerEle) == rightDstInnerEle;
        return rightMapped;
    }
}
