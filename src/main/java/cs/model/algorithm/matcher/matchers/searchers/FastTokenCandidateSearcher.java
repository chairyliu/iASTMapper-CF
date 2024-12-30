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

    public FastTokenCandidateSearcher(TokenElement srcToken, ElementMappings elementMappings,
                                      FilterDstCandidates filterDstCandidates, CandidateSetsAndMaps candidateSetsAndMaps) {
        this.srcToken = srcToken;
        this.elementMappings = elementMappings;
        this.filterDstCandidates = filterDstCandidates;
        this.candidateSetsAndMaps = candidateSetsAndMaps;
        this.sameTypeCandidates = candidateSetsAndMaps.getSameTypeDstCandidates(srcToken.getElementType());
    }

    public Set<ProgramElement> getSameStmtCandidateTokensForSrcToken() {
        SimMeasure measure = new TokenStmtMeasure();
        measure.setElementMappings(elementMappings);
        return measure.filterBadDstCandidateElements(srcToken, sameTypeCandidates, filterDstCandidates,candidateSetsAndMaps);
    }

    public Set<ProgramElement> getNeighborCandidateTokensForSrcToken() {
        SimMeasure measure = new TokenNeighborMeasure();
        measure.setElementMappings(elementMappings);
        return measure.filterBadDstCandidateElements(srcToken, sameTypeCandidates, filterDstCandidates,candidateSetsAndMaps);
    }

    public Set<ProgramElement> getSameValOrRenameCandidateTokensForSrcToken() {
        SimMeasure measure = new TokenSameRenameValueMeasure();
        measure.setElementMappings(elementMappings);
        return measure.filterBadDstCandidateElements(srcToken, sameTypeCandidates, filterDstCandidates,candidateSetsAndMaps);
    }

    public Set<ProgramElement> getCandidatesWithIdenticalMultiTokenForSrcToken() {
        SimMeasure measure = new TokenSameStructureMeasure();
        measure.setElementMappings(elementMappings);
        return measure.filterBadDstCandidateElements(srcToken, sameTypeCandidates, filterDstCandidates,candidateSetsAndMaps);
    }
}