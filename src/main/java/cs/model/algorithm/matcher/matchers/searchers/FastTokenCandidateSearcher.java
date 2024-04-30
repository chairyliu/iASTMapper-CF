package cs.model.algorithm.matcher.matchers.searchers;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.SimMeasure;
import cs.model.algorithm.matcher.measures.token.TokenNeighborMeasure;
import cs.model.algorithm.matcher.measures.token.TokenSameRenameValueMeasure;
import cs.model.algorithm.matcher.measures.token.TokenSameStructureMeasure;
import cs.model.algorithm.matcher.measures.token.TokenStmtMeasure;

import java.util.Set;

public class FastTokenCandidateSearcher {
    private final ElementMappings elementMappings;
    private final FilterDstCandidates filterDstCandidates;
    private final CandidateSetsAndMaps candidateSetsAndMaps;
    private TokenElement srcToken;
    private Set<ProgramElement> sameTypeCandidates;

    //针对给定的源令牌元素（TokenElement）在目标程序中查找不同类型的候选元素
    public FastTokenCandidateSearcher(TokenElement srcToken, ElementMappings elementMappings,
                                      FilterDstCandidates filterDstCandidates, CandidateSetsAndMaps candidateSetsAndMaps) {
        this.srcToken = srcToken;
        this.elementMappings = elementMappings;
        this.filterDstCandidates = filterDstCandidates;
        this.candidateSetsAndMaps = candidateSetsAndMaps;
        this.sameTypeCandidates = candidateSetsAndMaps.getSameTypeDstCandidates(srcToken.getElementType());
    }

    public Set<ProgramElement> getSameStmtCandidateTokensForSrcToken() {
        SimMeasure measure = new TokenStmtMeasure();//计算源令牌和目标令牌在语句中相似度的度量
        measure.setElementMappings(elementMappings);
        return measure.filterBadDstCandidateElements(srcToken, sameTypeCandidates, filterDstCandidates,candidateSetsAndMaps);//返回过滤后的候选目标元素集合
    }

    public Set<ProgramElement> getNeighborCandidateTokensForSrcToken() {
        SimMeasure measure = new TokenNeighborMeasure();//在邻近位置的相似度的度量
        measure.setElementMappings(elementMappings);
        return measure.filterBadDstCandidateElements(srcToken, sameTypeCandidates, filterDstCandidates,candidateSetsAndMaps);
    }

    public Set<ProgramElement> getSameValOrRenameCandidateTokensForSrcToken() {
        SimMeasure measure = new TokenSameRenameValueMeasure();//计算源令牌和目标令牌在值相同或重命名的相似度的度量
        measure.setElementMappings(elementMappings);
        return measure.filterBadDstCandidateElements(srcToken, sameTypeCandidates, filterDstCandidates,candidateSetsAndMaps);
    }

    public Set<ProgramElement> getCandidatesWithIdenticalMultiTokenForSrcToken() {
        SimMeasure measure = new TokenSameStructureMeasure();//在结构上相似度的度量
        measure.setElementMappings(elementMappings);
        return measure.filterBadDstCandidateElements(srcToken, sameTypeCandidates, filterDstCandidates,candidateSetsAndMaps);
    }
}