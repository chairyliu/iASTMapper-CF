package cs.model.algorithm.matcher.measures.token;

import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.matchers.searchers.CandidateSetsAndMaps;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Mechanism: if neighbor token is also mapped, the two tokens are more likely to be mapped.
 */
public class Token_LRBMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        double val = 0;
        TokenElement srcTokenEle = (TokenElement) srcEle;
        TokenElement dstTokenEle = (TokenElement) dstEle;
        // 获取传入token的上级stmt语句下的所有tokens元素
        List<TokenElement> srcTokenElements = srcTokenEle.getStmtElement().getTokenElements();
        List<TokenElement> dstTokenElements = dstTokenEle.getStmtElement().getTokenElements();

        // 如果只有一个token，则就是当前的srcTokenEle。但T-ABS是针对于传入token的邻居tokens，如果只有一个token就没有必要讨论
        if (srcTokenElements.size() == 1 && dstTokenElements.size() == 1 && !isParentMapping(srcEle, dstEle))
            return val;

        // Check if node of the token is the statement node.
        // Do not map two tokens with sandwich measure when node of the token is the statement node
        // 如果是stmt级别的token，就不匹配
        if (srcTokenEle.getStmtElement().getITreeNode() == srcTokenEle.getITreeNode()) {
            if (dstTokenEle.getStmtElement().getITreeNode() == dstTokenEle.getITreeNode()) {
                if (!isParentMapping(srcEle, dstEle)) {
                    return val;
                }
            }
        }

        boolean leftMapped = isLeftMapped(srcTokenEle, dstTokenEle);
        boolean rightMapped = isRightMapped(srcTokenEle, dstTokenEle);
        if(leftMapped && rightMapped)
            return 2.0;

        if (leftMapped || rightMapped)
            return 1.0;

        return val;
    }

    private boolean isLeftMapped(TokenElement srcTokenEle, TokenElement dstTokenEle) {//有两种情况，一种是直接获取传入toekn的子元素，另一种是借助inner对比
        //获取传入token的子元素索引
        int srcChildIdx = srcTokenEle.getChildIdx();
        int dstChildIdx = dstTokenEle.getChildIdx();
        //如果没有孩子节点，直接返回true，认为左节点匹配
        if (srcChildIdx == 0 && dstChildIdx == 0)
            return true;
        if (srcChildIdx > 0 && dstChildIdx > 0) {
            List<TokenElement> srcTokenElements = srcTokenEle.getStmtElement().getTokenElements();
            List<TokenElement> dstTokenElements = dstTokenEle.getStmtElement().getTokenElements();
            ProgramElement srcEle1 = srcTokenElements.get(srcChildIdx - 1);
            ProgramElement dstEle1 = dstTokenElements.get(dstChildIdx - 1);
            if (elementMappings.getMappedElement(srcEle1) == dstEle1)
                return true;
        }
        if (srcTokenEle.isVarName() || srcTokenEle.isLiteral() || isParentMapping(srcTokenEle, dstTokenEle)) {
            InnerStmtElement srcInnerStmtEle = srcTokenEle.getNearestMultiTokenInnerStmtElement();
            InnerStmtElement dstInnerStmtEle = dstTokenEle.getNearestMultiTokenInnerStmtElement();
            if (srcInnerStmtEle != null && dstInnerStmtEle != null &&
                    !srcInnerStmtEle.isNullElement() && !dstInnerStmtEle.isNullElement()) {
                //最邻近的多token内部语句 下面 的第一个token元素，等于当前传入的token，则返回true，认为左匹配
                if (srcInnerStmtEle.getTokenElements().get(0) == srcTokenEle)
                    if (dstInnerStmtEle.getTokenElements().get(0) == dstTokenEle)
                        return true;
            }
        }
        return false;
    }

    private boolean isRightMapped(TokenElement srcTokenEle, TokenElement dstTokenEle) {
        int srcChildIdx = srcTokenEle.getChildIdx();
        int dstChildIdx = dstTokenEle.getChildIdx();
        List<TokenElement> srcTokenElements = srcTokenEle.getStmtElement().getTokenElements();
        List<TokenElement> dstTokenElements = dstTokenEle.getStmtElement().getTokenElements();
        if (srcChildIdx  == srcTokenElements.size() - 1 && dstChildIdx == dstTokenElements.size() - 1)
            return true;
        if (srcChildIdx < srcTokenElements.size() - 1 && dstChildIdx < dstTokenElements.size() - 1) {
            ProgramElement srcEle2 = srcTokenElements.get(srcChildIdx + 1);
            ProgramElement dstEle2 = dstTokenElements.get(dstChildIdx + 1);
            if (elementMappings.getDstForSrc(srcEle2) == dstEle2)
                return true;
        }
        if (srcTokenEle.isVarName() || srcTokenEle.isLiteral() || isParentMapping(srcTokenEle, dstTokenEle)) {
            InnerStmtElement srcCE = srcTokenEle.getNearestMultiTokenInnerStmtElement();
            InnerStmtElement dstCE = dstTokenEle.getNearestMultiTokenInnerStmtElement();
            if (srcCE != null && dstCE != null && !srcCE.isNullElement() && !dstCE.isNullElement()) {
                List<TokenElement> srcTokenElements2 = srcCE.getTokenElements();
                List<TokenElement> dstTokenElements2 = dstCE.getTokenElements();
                if (srcTokenElements2.get(srcTokenElements2.size() - 1) == srcTokenEle)
                    if (dstTokenElements2.get(dstTokenElements2.size() - 1) == dstTokenEle)
                        return true;
            }
        }
        return false;
    }

    @Override
    public Set<ProgramElement> filterBadDstCandidateElements(ProgramElement srcEle, Set<ProgramElement> dstCandidates,
                                                             CandidateSetsAndMaps candidateSetsAndMaps) {
        if (!srcEle.isFromSrc())
            return null;

        Set<ProgramElement> neighborCandidates = new HashSet<>();
        if (dstCandidates.size() == 0)
            return neighborCandidates;
        TokenElement srcToken = (TokenElement) srcEle;
        StmtElement srcStmt = srcToken.getStmtElement();
        if (elementMappings.isMapped(srcStmt) && srcStmt.getTokenElements().size() == 1) {
            ProgramElement dstStmt = elementMappings.getMappedElement(srcStmt);
            if (dstStmt.getTokenElements().size() == 1) {
                neighborCandidates.add(dstStmt.getTokenElements().get(0));
            }
        }

        ProgramElement leftToken = srcToken.getLeftSibling();
        ProgramElement rightToken = srcToken.getRightSibling();
        if (leftToken != null && elementMappings.isMapped(leftToken)) {
            ProgramElement mappedLeftToken = elementMappings.getMappedElement(leftToken);
            ProgramElement nextToken = mappedLeftToken.getRightSibling();
            if (nextToken != null)
                neighborCandidates.add(nextToken);
        }

        if (rightToken != null && elementMappings.isMapped(rightToken)) {
            ProgramElement mappedRightToken = elementMappings.getMappedElement(rightToken);
            ProgramElement lastToken = mappedRightToken.getLeftSibling();
            if (lastToken != null)
                neighborCandidates.add(lastToken);
        }
        neighborCandidates.retainAll(dstCandidates);
        return neighborCandidates;
    }
}
