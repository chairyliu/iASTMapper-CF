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
        this.filterDstCandidates = filterDstCandidates;//新增
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

    public Map<ElementMapping, ElementSimMeasures> getBestSimMeasuresMap() {//无论是局部还是全局元素，遍历其候选集，找到匹配的<src,dst>作为键，获取相似度作为值，将这个map集合返回
        Map<ElementMapping, ElementSimMeasures> bestMeasuresMap = new HashMap<>();//用于存储最佳相似度度量的映射
        Set<ProgramElement> bestCandidates = new HashSet<>();//用于存储全局和局部最佳候选元素的集合
        if (bestGlobalCandidates != null)//如果全局最佳候选元素集合不为空，将其添加到最佳候选元素的集合中
            bestCandidates.addAll(bestGlobalCandidates);
        if (bestLocalCandidates != null)
            bestCandidates.addAll(bestLocalCandidates);
        for (ProgramElement candidate: bestCandidates) {
            ElementMapping mapping = getMappingObject(candidate);//根据当前对象element和输入的候选元素candidate，分辨哪个是src哪个是dst并建立映射关系
            ElementSimMeasures measures = getSimMeasures(candidate);//计算相似度值，第二套规则按顺序，只要有一个满足即可
            bestMeasuresMap.put(mapping, measures);//把映射（临时映射）<srcEle, dstEle>和对应的相似性度量值匹配在一起
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

    //根据相似性度量找全局和局部最优候选集（先全局后局部）
    private void findBestCandidates() {
        // 1. Find the best candidates from all the elements of the target file
        ElementSimMeasures mappedMeasures = null;
        if (eleMappings.isMapped(element)) {//如果当前元素已经有映射关系，那么就建立全局最优候选集
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
                break;//如果找到了一个或者一个都没找到全局最优，就跳出当前for，说明根据measureName筛选到了唯一的全局最优或者都不满足
        }

        // No candidate is found.
        // No need to calculate the best candidates excluding current mappings.
        if (bestGlobalCandidates.size() == 0)//如果把第二套所有的measureName都遍历了一遍仍然找不到全局最优，那就说明找不到，直接return
            return;

        //能进到这一步说明当前全局最有候选集中是有>1个元素的
        // 2. Find the best candidates excluding the elements that are mapped.
        this.bestLocalCandidates = new HashSet<>();
        if (!eleMappings.isMapped(element)) {//如果当前元素没有映射，就建立局部最优候选集
            // We check if the global candidates contain elements that are not mapped.
            this.bestLocalCandidates = MatcherUtil
                    .getUnmappedProgramElements(this.bestGlobalCandidates, eleMappings);//筛选全局最佳候选元素中未映射的元素作为局部候选集

            // If the global candidates do not contain unmapped elements,
            // find candidates excluding current mappings.
            if (this.bestLocalCandidates.size() == 0) {//如果全局最佳候选元素不包含未映射的元素
                this.bestLocalCandidates = MatcherUtil
                        .getUnmappedProgramElements(candidateElements, eleMappings);//则从初始化的候选集中找未映射的元素
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

    //用determine和doCompare迭代筛选全局/局部候选集中的元素，ret可能有一个可能有1个元素，也可能有0个元素，也可能有多个元素
    private Set<ProgramElement> findBestCandidates(String measureName,
                                                   Set<ProgramElement> candidateElements,
                                                   ElementSimMeasures mappedMeasures) {
        Set<ProgramElement> ret = new HashSet<>();//用于存储当前element对应的最佳候选元素集
        ElementSimMeasures bestMeasures = null;//用于存储最佳相似度度量的对象
        //如果已映射元素的相似度度量存在，并且确定要选择该元素
        if (mappedMeasures != null && determiner.determine(mappedMeasures)){//mappedMeasures应该是相似性度量的值
            ret.add(mappedMeasures.getAnotherElement(element));//获取与element相关的另一个元素（src或dst），并存入ret集合中
            bestMeasures = mappedMeasures;
        }
        for (ProgramElement candidate: candidateElements) {//遍历传入的初始局部/全局候选集
            ElementSimMeasures measures = getSimMeasures(candidate);//获取当前候选元素的相似度度量
            if (ret.size() == 0) {//如果ret为空，就说明当前的element元素与它已经匹配好的src/dst是不满足determine的
                if (determiner.determine(measures)) {//所以要遍历候选集拿到与每个候选元素的相似值，并用determine度量
                    bestMeasures = measures;//如果determine成立，那么认为当前度量值最佳
                    ret.add(candidate);//在ret中添加当前的候选元素
                }
            } else {
                //用doCompare衡量bestMeasures与传入的measures哪个更好
                int cmp = ElementSimMeasures.doCompare(measures, bestMeasures, eleMappings, measureName);
                if (cmp == 1) {//如果当前元素的相似度measures大于最佳相似度度量bestMeasures
                    if (determiner.determine(measures)) {//由determine做决定选哪个
                        bestMeasures = measures;
                        ret.clear();
                        ret.add(candidate);
                    }
                } else if (cmp == 0) {//如果当前元素的相似度小于最佳相似度度量
                    if (determiner.determine(measures))//如果满足determine则加入ret
                        ret.add(candidate);
                }
            }
        }
//        System.out.println(ret);
        return ret;
    }
}
