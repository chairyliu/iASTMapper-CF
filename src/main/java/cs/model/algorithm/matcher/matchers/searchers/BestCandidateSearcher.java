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

    public BestCandidateSearcher(ProgramElement element, Set<ProgramElement> candidateElements,
                                 ElementMappings eleMappings, ElementMatchDeterminer determiner,
                                 CandidateSetsAndMaps candidateSetsAndMaps) {
        this.element = element;
        this.candidateElements = candidateElements;
        this.eleMappings = eleMappings;
        this.simMeasureConfiguration = SimMeasureConfiguration.getSimilarityMeasureConfiguration(element);
        this.determiner = determiner;
        this.candidateSetsAndMaps = candidateSetsAndMaps;
        this.measuresMap = new HashMap<>();
        findBestCandidates();
    }

    public BestCandidateSearcher(ProgramElement element,
                                 Set<ProgramElement> candidateElements,
                                 ElementMappings eleMappings,
                                 Map<ElementMapping, ElementSimMeasures> measuresMap,
                                 ElementMatchDeterminer determiner,
                                 CandidateSetsAndMaps candidateSetsAndMaps) {
        this.element = element;
        this.candidateElements = candidateElements;
        this.eleMappings = eleMappings;
        this.measuresMap = measuresMap;
        this.determiner = determiner;
        this.simMeasureConfiguration = SimMeasureConfiguration.getSimilarityMeasureConfiguration(element);
        this.candidateSetsAndMaps = candidateSetsAndMaps;
        findBestCandidates();
    }

    public Set<ProgramElement> getBestGlobalCandidates() {
        return bestGlobalCandidates;
    }

    public Set<ProgramElement> getBestLocalCandidates() {
        return bestLocalCandidates;
    }

    public Map<ElementMapping, ElementSimMeasures> getBestSimMeasuresMap() {
        Map<ElementMapping, ElementSimMeasures> bestMeasuresMap = new HashMap<>();//用于存储最佳相似度度量的映射
        Set<ProgramElement> bestCandidates = new HashSet<>();//用于存储全局和局部最佳候选元素的集合
        if (bestGlobalCandidates != null)//如果全局最佳候选元素集合不为空，将其添加到最佳候选元素的集合中
            bestCandidates.addAll(bestGlobalCandidates);
        if (bestLocalCandidates != null)
            bestCandidates.addAll(bestLocalCandidates);
        for (ProgramElement candidate: bestCandidates) {
            ElementMapping mapping = getMappingObject(candidate);//获取候选元素对应的映射对象，如果candidate是src，就获取dst，如果是dst，就获取src
            ElementSimMeasures measures = getSimMeasures(candidate);
            bestMeasuresMap.put(mapping, measures);//把映射（临时映射）和对应的相似性度量值匹配在一起
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

    //根据相似性度量得到最佳目标候选元素（先全局后局部）
    private void findBestCandidates() {
        // 1. Find the best candidates from all the elements of the target file
        ElementSimMeasures mappedMeasures = null;
        if (eleMappings.isMapped(element)) {//如果当前元素已经有映射关系
            ProgramElement mappedEle = eleMappings.getMappedElement(element);//获取已映射元素
            mappedMeasures = getSimMeasures(mappedEle);//获取与映射元素相关的相似度度量
        }

        this.bestGlobalCandidates = new HashSet<>();//初始化全局最佳候选元素集合
        if (candidateElements != null)
            this.bestGlobalCandidates.addAll(candidateElements);//如果候选元素集合不为空，将其添加到全局最佳候选元素集合中

        for (String measureName: simMeasureConfiguration) {//对于每个相似度度量配置，将度量名称添加到 iASTMapper.used_rules 中
            iASTMapper.used_rules.add(measureName);  // 添加-ZN
            //在当前相似度度量下，找到全局最佳候选元素集合（找相似度值最大的）
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
        if (!eleMappings.isMapped(element)) {//如果当前元素没有映射
            // We check if the global candidates contain elements that are not mapped.
            this.bestLocalCandidates = MatcherUtil
                    .getUnmappedProgramElements(this.bestGlobalCandidates, eleMappings);//检查全局最佳候选元素中是否包含未映射的元素

            // If the global candidates do not contain unmapped elements,
            // find candidates excluding current mappings.
            if (this.bestLocalCandidates.size() == 0) {//如果全局最佳候选元素不包含未映射的元素
                this.bestLocalCandidates = MatcherUtil
                        .getUnmappedProgramElements(candidateElements, eleMappings);//找到不包括当前映射的候选元素
                for (String measureName: simMeasureConfiguration) {
                    iASTMapper.used_rules.add(measureName);  // 添加-ZN
                    this.bestLocalCandidates = findBestCandidates(measureName,
                            this.bestLocalCandidates, null);//在当前相似度度量下，找到不包括已映射元素的最佳候选元素集合
                    if (this.bestLocalCandidates.size() <= 1)
                        break;
                }
            }
        }
    }

    private Set<ProgramElement> findBestCandidates(String measureName,
                                                   Set<ProgramElement> candidateElements,
                                                   ElementSimMeasures mappedMeasures) {
        Set<ProgramElement> ret = new HashSet<>();//用于存储最佳候选元素的集合
        ElementSimMeasures bestMeasures = null;//用于存储最佳相似度度量的对象
        //如果已映射元素的相似度度量存在，并且确定要选择该元素
        if (mappedMeasures != null && determiner.determine(mappedMeasures)){//mappedMeasures应该是相似性度量的值
            ret.add(mappedMeasures.getAnotherElement(element));//获取与element相关的另一个元素（src或dst）
            bestMeasures = mappedMeasures;
        }
        for (ProgramElement candidate: candidateElements) {//遍历候选元素集合
            ElementSimMeasures measures = getSimMeasures(candidate);//获取当前候选元素的相似度度量
            if (ret.size() == 0) {//如果ret为空，就根据determine判断更新最佳相似度度量和最佳候选元素集合
                if (determiner.determine(measures)) {
                    bestMeasures = measures;
                    ret.add(candidate);
                }
            } else {
                int cmp = ElementSimMeasures.doCompare(measures, bestMeasures, eleMappings, measureName);
                if (cmp == 1) {//如果当前元素的相似度大于最佳相似度度量
                    if (determiner.determine(measures)) {//由determine做决定选哪个
                        bestMeasures = measures;
                        ret.clear();
                        ret.add(candidate);
                    }
                } else if (cmp == 0) {//如果当前元素的相似度与最佳相似度度量相等
                    if (determiner.determine(measures))
                        ret.add(candidate);
                }
            }
        }
        return ret;
    }
}
