package cs.model.algorithm.matcher.matchers.searchers;

import cs.model.algorithm.element.*;
import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.rules.ElementMatchDeterminer;

import java.util.*;

/**
 * The class for searching candidates for a given
 * source statement, inner-stmt element or token.
 */
public class CandidateSearcher {//建立语句、内部语句、token的候选集

    private CandidateSetsAndMaps candidateSetsAndMaps;
    private ElementMappings elementMappings;


    public CandidateSearcher(CandidateSetsAndMaps candidateSetsAndMaps, ElementMappings elementMappings) {
        this.candidateSetsAndMaps = candidateSetsAndMaps;
        this.elementMappings = elementMappings;
    }
    public CandidateSetsAndMaps getCandidateSetsAndMaps() {
        return candidateSetsAndMaps;
    }

    /**
     * Set current element mappings each time the element mappings are updated
     * @param elementMappings current element mappings
     */
    public void setElementMappings(ElementMappings elementMappings) {
        this.elementMappings = elementMappings;
    }

    /**
     * Get the target candidates for a given source element
     * @param srcElement a given source element
     * @return the set of target candidates
     */
    //获取给定源元素的目标候选项，并强制转换为与srcElement对应的类型（三种）
    public Set<ProgramElement> getDstCandidateElements(ProgramElement srcElement) {
        if (srcElement.isStmt())
            return getDstCandidateStmtElements((StmtElement) srcElement);
        else if (srcElement.isToken())
            return getDstCandidateTokenElements((TokenElement) srcElement);
        else
            return getDstCandidateInnerStmtElements((InnerStmtElement) srcElement);
    }

    /**
     * Get source statements that are not mapped by fast matchers
     */
    public Set<ProgramElement> getSrcStmtsToMap() {
        return candidateSetsAndMaps.getSrcStmtsToMap();
    }

    /**
     * Get source tokens that are not mapped by fast matchers
     */
    public Set<ProgramElement> getSrcTokensToMap() {
        return candidateSetsAndMaps.getSrcTokensToMap();
    }

    /**
     * Get all the source statements in the file
     */
    public Set<ProgramElement> getAllSrcStmts() {
        return candidateSetsAndMaps.getAllSrcStmts();
    }


    private Map<ProgramElementType, Set<ProgramElement>> getDstTypeElementMap() {
        return candidateSetsAndMaps.getDstTypeElementMap();
    }

    private Set<ProgramElement> getDstCandidateStmtElements(StmtElement srcStmt) {
        return getDstTypeElementMap().get(srcStmt.getElementType());
    }

    //获取内部语句下的token（源），遍历srcTokens，找到对应的elementMappings中映射的dstToken，获取和该token相关联的内部语句
    //连同内部语句的类型一起存入ret。再将原内部语句已经映射父元素的所有内部语句都存入ret，返回ret
    private Set<ProgramElement> getDstCandidateInnerStmtElements(InnerStmtElement srcInnerStmtEle) {
        List<TokenElement> srcTokens = srcInnerStmtEle.getTokenElements();//获取源InnerStmtElement关联的token元素列表
        Set<ProgramElement> ret = new HashSet<>();
        ProgramElementType srcEleType = srcInnerStmtEle.getElementType();//获取内部语句的类型

        for (TokenElement token: srcTokens) {
            ProgramElement dstToken = elementMappings.getMappedElement(token);//对于每个token元素，通过elementMappings获取其映射的目标token
            if (dstToken != null) {
                //获取目标token元素关联的多个内部语句元素列表，并遍历这些内部语句
                List<InnerStmtElement> innerStmtElements = ((TokenElement) dstToken).getInnerStmtElementsWithToken();
                for (InnerStmtElement element: innerStmtElements){
                    ProgramElementType dstEleType = element.getElementType();//获取内部语句类型
                    if (srcEleType.equals(dstEleType))//如果与srcEle相同，添加到ret中
                        ret.add(element);
                }
            }
        }
        ProgramElement srcParentEle = srcInnerStmtEle.getParentElement();
        if (elementMappings.isMapped(srcParentEle)) {//检查srcInnerStmtEle的父元素是否被映射
            ProgramElement dstParentEle = elementMappings.getMappedElement(srcParentEle);
            ret.addAll(dstParentEle.getInnerStmtElements());//将目标父元素的所有内部语句元素添加到结果集合ret中。
        }
        return ret;
    }

    private Set<ProgramElement> getDstCandidateTokenElements(TokenElement srcToken) {
        FastTokenCandidateSearcher searcher = new FastTokenCandidateSearcher(srcToken, elementMappings,
                candidateSetsAndMaps);
        //通过searcher获取具有 相同结构 的多令牌候选集合
        Set<ProgramElement> candidatesWithSameStructure = searcher.getCandidatesWithIdenticalMultiTokenForSrcToken();
        //获取 相同语句 中的令牌候选集合
        Set<ProgramElement> sameStmtCandidates = searcher.getSameStmtCandidateTokensForSrcToken();
        //获取在相同语句中的候选集合，并存储在tmp中
        Set<ProgramElement> tmp = getDstCandidateTokenElementsInSameStmt(srcToken, sameStmtCandidates);
        if (tmp.size() > 0) {
            Set<ProgramElement> tmp2 = new HashSet<>(tmp);
            tmp2.retainAll(candidatesWithSameStructure);//检查tmp与相同结构的多令牌候选集合的交集
            if (tmp2.size() > 0) {//交集大于0
                if (checkGoodCandidatesCondition(srcToken, tmp2))//条件也满足返回tmp2
                    return tmp2;
            }

            if (checkGoodCandidatesCondition(srcToken, tmp))//交集为0或条件不满足，比较tmp，这时如果满足条件返回tmp
                return tmp;
        } else if (candidatesWithSameStructure.size() > 0) {
            if (checkGoodCandidatesCondition(srcToken, candidatesWithSameStructure))
                return candidatesWithSameStructure;
        }

        Set<ProgramElement> neighborCandidates = searcher.getNeighborCandidateTokensForSrcToken();
        Set<ProgramElement> sameOrRenameValCandidates = searcher.getSameValOrRenameCandidateTokensForSrcToken();
        Set<ProgramElement> ret = new HashSet<>();
        ret.addAll(tmp);
        ret.addAll(neighborCandidates);
        ret.addAll(sameOrRenameValCandidates);
        return ret;//考虑一些特殊情况，将上述tmp与这些特殊情况合并返回ret
    }

    //从在相同语句中的目标候选元素集合中筛选出与源令牌元素匹配的元素
    private Set<ProgramElement> getDstCandidateTokenElementsInSameStmt(TokenElement srcToken,
                                                                       Set<ProgramElement> sameStmtCandidates) {
        ElementMatchDeterminer determiner = new ElementMatchDeterminer(elementMappings);
        Set<ProgramElement> ret = new HashSet<>();
        for (ProgramElement element: sameStmtCandidates) {
            ElementSimMeasures measures = new ElementSimMeasures(srcToken, element);
            if (determiner.determine(measures))
                ret.add(element);
        }
        return ret;
    }

    private boolean checkGoodCandidatesCondition(TokenElement srcToken, Set<ProgramElement> candidates) {
        if (elementMappings.isMapped(srcToken)) {
            ProgramElement dstToken = elementMappings.getMappedElement(srcToken);//找到与srcToken映射的dstToken
            return candidates.contains(dstToken);
        } else {
            for (ProgramElement dst: candidates) {
                if (!elementMappings.isMapped(dst))
                    return true;
            }
            return false;
        }
    }
}
