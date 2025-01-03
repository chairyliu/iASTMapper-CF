package cs.model.algorithm.matcher.matchers.searchers;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.iASTMapper;
import cs.model.algorithm.matcher.mappings.ElementMapping;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.matchers.utils.MatcherUtil;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.measures.SimMeasureConfiguration;
import cs.model.algorithm.matcher.rules.ElementMatchDeterminer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BestCandidateSearcher {
    private final ProgramElement element;
    private final Set<ProgramElement> candidateElements;
    private final String[] simMeasureConfiguration;

    private ElementMappings eleMappings;
    private ElementMatchDeterminer determiner;

    // stored results
    private Map<ElementMapping, ElementSimMeasures> measuresMap;
    private Set<ProgramElement> bestGlobalCandidates;
    private Set<ProgramElement> bestLocalCandidates;
    private CandidateSetsAndMaps candidateSetsAndMaps;
    private FilterDstCandidates filterDstCandidates;

    public BestCandidateSearcher(ProgramElement element, Set<ProgramElement> candidateElements,
                                 ElementMappings eleMappings, ElementMatchDeterminer determiner,
                                 FilterDstCandidates filterDstCandidates, CandidateSetsAndMaps candidateSetsAndMaps) {
        this.element = element;
        this.candidateElements = candidateElements;
        this.eleMappings = eleMappings;
        this.simMeasureConfiguration = SimMeasureConfiguration.getSimilarityMeasureConfiguration(element);
        this.determiner = determiner;
        this.candidateSetsAndMaps = candidateSetsAndMaps;
        this.measuresMap = new HashMap<>();
        this.filterDstCandidates = filterDstCandidates;
        findBestCandidates();
    }

    public BestCandidateSearcher(ProgramElement element,
                                 Set<ProgramElement> candidateElements,
                                 ElementMappings eleMappings,
                                 Map<ElementMapping, ElementSimMeasures> measuresMap,
                                 ElementMatchDeterminer determiner,
                                 FilterDstCandidates filterDstCandidates, CandidateSetsAndMaps candidateSetsAndMaps) {
        this.element = element;
        this.candidateElements = candidateElements;
        this.eleMappings = eleMappings;
        this.measuresMap = measuresMap;
        this.determiner = determiner;
        this.simMeasureConfiguration = SimMeasureConfiguration.getSimilarityMeasureConfiguration(element);
        this.candidateSetsAndMaps = candidateSetsAndMaps;
        this.filterDstCandidates = filterDstCandidates;
        findBestCandidates();
    }

    public Set<ProgramElement> getBestGlobalCandidates() {
        return bestGlobalCandidates;
    }

    public Set<ProgramElement> getBestLocalCandidates() {
        return bestLocalCandidates;
    }

    public Map<ElementMapping, ElementSimMeasures> getBestSimMeasuresMap() {
        Map<ElementMapping, ElementSimMeasures> bestMeasuresMap = new HashMap<>();
        Set<ProgramElement> bestCandidates = new HashSet<>();
        if (bestGlobalCandidates != null)
            bestCandidates.addAll(bestGlobalCandidates);
        if (bestLocalCandidates != null)
            bestCandidates.addAll(bestLocalCandidates);
        for (ProgramElement candidate: bestCandidates) {
            ElementMapping mapping = getMappingObject(candidate);
            ElementSimMeasures measures = getSimMeasures(candidate);
            bestMeasuresMap.put(mapping, measures);
        }
        return bestMeasuresMap;
    }

    private ElementMapping getMappingObject(ProgramElement candidate) {
        ProgramElement srcEle = element;
        ProgramElement dstEle = candidate;
        if (!element.isFromSrc()) {
            srcEle = candidate;
            dstEle = element;
        }
        return new ElementMapping(srcEle, dstEle);
    }

    private ElementSimMeasures getSimMeasures (ProgramElement candidate) {
        ElementMapping mapping = getMappingObject(candidate);
        if (measuresMap.containsKey(mapping))
            return measuresMap.get(mapping);
        ElementSimMeasures measures = new ElementSimMeasures(mapping.first, mapping.second);
        measuresMap.put(mapping, measures);
        return measures;
    }

    private void findBestCandidates() {
        // 1. Find the best candidates from all the elements of the target file
        ElementSimMeasures mappedMeasures = null;
        if (eleMappings.isMapped(element)) {
            ProgramElement mappedEle = eleMappings.getMappedElement(element);
            mappedMeasures = getSimMeasures(mappedEle);
        }

        this.bestGlobalCandidates = new HashSet<>();
        if (candidateElements != null)
            this.bestGlobalCandidates.addAll(candidateElements);

        for (String measureName: simMeasureConfiguration) {
            iASTMapper.used_rules.add(measureName);

            this.bestGlobalCandidates = findBestCandidates(measureName, this.bestGlobalCandidates, mappedMeasures);
            if (this.bestGlobalCandidates.size() <= 1)
                break;
        }

        // No candidate is found.
        // No need to calculate the best candidates excluding current mappings.
        if (bestGlobalCandidates.size() == 0)
            return;

        // 2. Find the best candidates excluding the elements that are mapped.
        this.bestLocalCandidates = new HashSet<>();
        if (!eleMappings.isMapped(element)) {
            // We check if the global candidates contain elements that are not mapped.
            this.bestLocalCandidates = MatcherUtil
                    .getUnmappedProgramElements(this.bestGlobalCandidates, eleMappings);

            // If the global candidates do not contain unmapped elements,
            // find candidates excluding current mappings.
            if (this.bestLocalCandidates.size() == 0) {
                this.bestLocalCandidates = MatcherUtil
                        .getUnmappedProgramElements(candidateElements, eleMappings);
                for (String measureName: simMeasureConfiguration) {
                    iASTMapper.used_rules.add(measureName);
                    this.bestLocalCandidates = findBestCandidates(measureName,
                            this.bestLocalCandidates, null);
                    if (this.bestLocalCandidates.size() <= 1)
                        break;
                }
            }
        }
    }

    private Set<ProgramElement> findBestCandidates(String measureName,
                                                   Set<ProgramElement> candidateElements,
                                                   ElementSimMeasures mappedMeasures) {
        Set<ProgramElement> ret = new HashSet<>();
        ElementSimMeasures bestMeasures = null;
        if (mappedMeasures != null && determiner.determine(mappedMeasures)){
            ret.add(mappedMeasures.getAnotherElement(element));
            bestMeasures = mappedMeasures;
        }
        for (ProgramElement candidate: candidateElements) {
            ElementSimMeasures measures = getSimMeasures(candidate);
            if (ret.size() == 0) {
                if (determiner.determine(measures)) {
                    bestMeasures = measures;
                    ret.add(candidate);
                }
            } else {
                int cmp = ElementSimMeasures.doCompare(measures, bestMeasures, eleMappings, measureName);
                if (cmp == 1) {
                    if (determiner.determine(measures)) {
                        bestMeasures = measures;
                        ret.clear();
                        ret.add(candidate);
                    }
                } else if (cmp == 0) {
                    if (determiner.determine(measures))
                        ret.add(candidate);
                }
            }
        }
        return ret;
    }
}
