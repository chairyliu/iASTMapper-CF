package cs.model.algorithm.matcher.matchers.searchers;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMapping;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.rules.ElementMatchDeterminer;
import cs.model.algorithm.utils.LongestCommonSubsequence;

import java.util.*;

/**
 * Search the best element pairs to map
 *
 * We have three settings:
 * 1. Consider all the target elements when searching the best element to map (one-to-one)
 * 2. Search one-to-one best mapped element pairs excluding current mappings
 * 3. Search multi-to-multi best mapped element pairs excluding current mappings
 */
public class BestMappingSearcher {
    // searcher for the candidates of a given source element
    private final CandidateSearcher candidateSearcher;

    // data structure for measures that need to be stored
    private final Map<ElementMapping, ElementSimMeasures> measuresMap;

    // global candidate, consider all the elements in the file
    private final Map<ProgramElement, Set<ProgramElement>> srcToGlobalBestDstCandidateMap;

    // local candidate, excluding the mapped elements
    private final Map<ProgramElement, Set<ProgramElement>> srcToLocalBestDstCandidateMap;

    // best source candidates for each target element excluding mapped elements
    private final Map<ProgramElement, Set<ProgramElement>> dstToLocalBestSrcCandidateMap;

    // the element mappings
    private ElementMappings eleMappings;

    // determiner that decides if two elements can be mapped
    private ElementMatchDeterminer determiner;

    public BestMappingSearcher(CandidateSearcher candidateSearcher){
        this.candidateSearcher = candidateSearcher;
        this.measuresMap = new HashMap<>();
        this.srcToGlobalBestDstCandidateMap = new HashMap<>();
        this.srcToLocalBestDstCandidateMap = new HashMap<>();
        this.dstToLocalBestSrcCandidateMap = new HashMap<>();
    }

    public void setElementMappings(ElementMappings eleMappings) {
        this.eleMappings = eleMappings;
        this.candidateSearcher.setElementMappings(eleMappings);
    }

    public void setElementMatchDeterminer(ElementMatchDeterminer determiner) {
        this.determiner = determiner;
    }

    public void clearBestCandidateMaps() {
        this.srcToGlobalBestDstCandidateMap.clear();
        this.srcToLocalBestDstCandidateMap.clear();
        this.dstToLocalBestSrcCandidateMap.clear();
        this.measuresMap.clear();
    }

    public ElementSimMeasures getElementSimMeasures(ProgramElement srcEle, ProgramElement dstEle) {
        return measuresMap.get(new ElementMapping(srcEle, dstEle));
    }

    public Set<ProgramElement> getSrcStmtsToMap() {
        return candidateSearcher.getSrcStmtsToMap();
    }//在快速映射阶段没有匹配的stmt

    public Set<ProgramElement> getSrcTokensToMap() {
        return candidateSearcher.getSrcTokensToMap();
    }

    public Set<ProgramElement> getAllSrcStmts() {
        return candidateSearcher.getAllSrcStmts();
    }

    /**
     * Find the element pairs to map
     * @param elementsToMap data structure to store element pairs to map
     * @param excludeCurMappings whether to exclude current mappings
     */
    public void findElementPairsToMap(Map<ProgramElement, ProgramElement> elementsToMap,
                                      Set<ProgramElement> allSrcElements,
                                      boolean excludeCurMappings) {
        // First find the best target candidates for all the source elements
        // 1. consider all the target candidates
        // 2. consider candidates excluding current mappings
        for (ProgramElement srcElement: allSrcElements)//allSrcElements是在快速映射阶段没有被映射的所有stmt集合
            findBestDstCandidates(srcElement);//获取srcElement的最佳目标候选集，全局没有就局部
        // Then, we find the one-to-one and multi-to-multi best mappings of elements.
        if (!excludeCurMappings) {//false代表全局，true代表局部
            Map<ProgramElement, Set<ProgramElement>> myLocal = new HashMap<>();//一个目标元素对应一个源集合，以map形式统一存储
            findOneToOneBestElementPairsToMap(elementsToMap, srcToGlobalBestDstCandidateMap, myLocal);
        } else {
            // Find one-to-one best mappings and exclude current mappings
            findOneToOneBestElementPairsToMap(elementsToMap, srcToLocalBestDstCandidateMap, dstToLocalBestSrcCandidateMap);

            if (elementsToMap.size() > 0)
                return;

            // Find multiple-to-multiple mappings and exclude current mappings
            findBestMultiMappings(elementsToMap);
        }
    }

    private void findBestDstCandidates(ProgramElement srcElement) {//获取传入元素的候选集，搜寻是否有全局最佳匹配，若没有找局部最佳的集合，并将<<src,dst>,sim>存入map中
        //获取与源元素（srcElement）相关的目标元素候选集合（上一阶段的产物），表示如果该元素都没有最初始的候选集，就不需要进行后续的判断了，直接返回
        Set<ProgramElement> candidateElements = candidateSearcher.getDstCandidateElements(srcElement);
        if (candidateElements == null || candidateElements.size() == 0)
            return;
        //使用BestCandidateSearcher找到与srcElement最相似的目标元素，并将结果存储在globalBestDstCandidates变量中。
        BestCandidateSearcher searcher = new BestCandidateSearcher(srcElement, candidateElements,
                eleMappings, determiner, candidateSearcher.getFilterDstCandidates(),candidateSearcher.getCandidateSetsAndMaps());
        Set<ProgramElement> globalBestDstCandidates = searcher.getBestGlobalCandidates();
//        if (srcElement.getStringValue().equals("package org activeio net"))
//            System.out.println(globalBestDstCandidates);
        //如果srcElement未被映射，也就说明上两行没有找到全局最佳元素，所以获取局部最佳目标元素候选集合，并将其和srcElement绑定存到局部的map集合中
        if (!eleMappings.isMapped(srcElement)) {
            Set<ProgramElement> localBestDstCandidates = searcher.getBestLocalCandidates();
            if (localBestDstCandidates != null)
                srcToLocalBestDstCandidateMap.put(srcElement, localBestDstCandidates);
        }
        //如果找到了全局最佳，和srcElement绑定存储在srcToGlobalBestDstCandidateMap中
        srcToGlobalBestDstCandidateMap.put(srcElement, globalBestDstCandidates);
        //将测量值和临时映射存储在measuresMap中
        measuresMap.putAll(searcher.getBestSimMeasuresMap());
    }

    //首先获取两个全局的候选项集合（源到目标，目标到源）
    //1.如果目标元素到源只有一个候选项，源也只有一个到目标的候选项，检查src和dst是否已相互映射，如果没有，则添加到elementsToMap中，如果已映射则将src添加到dst传入的最优候选集中
    //2.如果目标元素有多个源候选项，调用BestCandidateSearcher拿到传入dstEle对应的全局最佳候选集，如果该候选集不为空，将其作为dstELe的值存到传入的dst候选集map中。
    // 如果该全局最佳候选集只有一个元素，且该src元素的候选集中只有一个dst候选元素，且二者没有互相匹配，那么将该src与dst配对存入elementsToMap
    //对应于 全局一对一 和 局部一对一 的情况，具体要看传入的参数是局部还是全局
    private void findOneToOneBestElementPairsToMap(Map<ProgramElement, ProgramElement> elementsToMap,
                                                  Map<ProgramElement, Set<ProgramElement>> srcToBestDstCandidateMap,
                                                  Map<ProgramElement, Set<ProgramElement>> dstToBestSrcCandidateMap) {
        //对于传入的src的候选集（无论是局部还是全局），首先遍历每个src元素，拿到对应的dst元素，再检查新建的dst候选集中包不包括该dst，不包括的话在map集合中新增该dst作为key。最后将src这个元素加入到dst键对应的值集合中
        //其中src的候选集是传入的参数，而dst的候选集是新建的。这样就建立了src候选集中每个dst元素到src的映射
        Map<ProgramElement, Set<ProgramElement>> dstToGlobalSrcCandidateMap = new HashMap<>();//每个目标元素的所有源候选项
        for (ProgramElement srcElement: srcToBestDstCandidateMap.keySet()) {//遍历源到最佳目标的候选项映射，得到srcElement
            Set<ProgramElement> dstElements = srcToBestDstCandidateMap.get(srcElement);//找到与每个源对应的最佳目标候选集
            //遍历上一步的目标候选集，如果 目标到全局源 的候选集不包含该dstElement，则添加至列表
            for (ProgramElement dstElement: dstElements) {
                if (!dstToGlobalSrcCandidateMap.containsKey(dstElement))
                    dstToGlobalSrcCandidateMap.put(dstElement, new HashSet<>());
                dstToGlobalSrcCandidateMap.get(dstElement).add(srcElement);
            }
        }
        //遍历目标到全局源的候选项映射
        for (ProgramElement dstElement: dstToGlobalSrcCandidateMap.keySet()) {
            //如果目标元素只有一个源候选项
            if (dstToGlobalSrcCandidateMap.get(dstElement).size() == 1) {
                ProgramElement srcElement = dstToGlobalSrcCandidateMap.get(dstElement).iterator().next();//获取集合中的第一个元素
                // 如果源元素只有一个目标候选项
                if (srcToBestDstCandidateMap.get(srcElement).size() == 1) {
                    if (eleMappings.getMappedElement(srcElement) != dstElement)
                        elementsToMap.put(srcElement, dstElement);//不映射则添加
                    else {
                        //如果源元素已经映射到该目标元素，则存储在dstToBestSrcCandidateMap中
                        Set<ProgramElement> candidates = new HashSet<>();
                        candidates.add(srcElement);
                        dstToBestSrcCandidateMap.put(dstElement, candidates);
                    }
                    continue;
                }
            }
            //如果目标元素有多个源候选项，则使用BestCandidateSearcher找到最佳的源元素候选项
            Set<ProgramElement> candidates = dstToGlobalSrcCandidateMap.get(dstElement);
            BestCandidateSearcher searcher = new BestCandidateSearcher(dstElement, candidates,
                    eleMappings, measuresMap, determiner, candidateSearcher.getFilterDstCandidates(),candidateSearcher.getCandidateSetsAndMaps());
//            System.out.println(searcher);
            //存储最佳的源元素候选项映射
            Set<ProgramElement> bestSrcCandidates = searcher.getBestGlobalCandidates();
            if (bestSrcCandidates != null) {
                dstToBestSrcCandidateMap.put(dstElement, bestSrcCandidates);
                //如果最佳的源元素候选项只有一个，并且该源元素只有一个目标候选项，则进行映射
                if (bestSrcCandidates.size() == 1) {
                    ProgramElement srcElement = bestSrcCandidates.iterator().next();
                    if (srcToBestDstCandidateMap.containsKey(srcElement) && srcToBestDstCandidateMap.get(srcElement).size() == 1) {
                        if (eleMappings.getMappedElement(srcElement) != dstElement)
                            elementsToMap.put(srcElement, dstElement);
                    }
                }
            }
        }
    }

    /**
     * Mapping the multi-to-multi cases in lcs order.
     */
    //根据源元素到局部最佳目标元素的候选集合以及目标元素到局部最佳源元素的候选集合，找到最佳的多对映射
    //对应于局部多对多的情况：如果srcEle的候选集中有dstEle，反之dstEle的候选集中也有srcEle，则将srcEle,dstEle对存入goodMappings中
    private void findBestMultiMappings(Map<ProgramElement, ProgramElement> elementsToMap) {
        Set<ProgramElement> srcElements = new HashSet<>();
        Set<ProgramElement> dstElements = new HashSet<>();
        Set<ElementMapping> goodMappings = new HashSet<>();

        //这个大的for循环：对于src的局部候选集，遍历每个src拿到其对应的dst局部候选集。然后再对这个dst候选集遍历，对每个dstEle，都去dstToLocalBestSrcCandidateMap中get到对应的src候选集
        //然后检查这个src候选集中包不包含当前的srcEle（相当于一个反向验证）。如果包含，则将当前srcEle和dstEle建立映射存入goodMappings中
        //对于每个源元素，遍历其对应的局部最佳目标元素候选集合
        for (ProgramElement srcElement: srcToLocalBestDstCandidateMap.keySet()) {
            //获取每个源元素到最佳目标元素的候选集合
            Set<ProgramElement> bestDstElements = srcToLocalBestDstCandidateMap.get(srcElement);
            //对于每个最佳目标元素，遍历其对应的最佳源元素候选集合
            for (ProgramElement dstElement: bestDstElements) {
                //获取目标元素到最佳源元素的候选集合
                Set<ProgramElement> bestSrcElements = dstToLocalBestSrcCandidateMap.get(dstElement);
                //如果最佳源元素集合不为空，且包含当前源元素，说明找到了一个好的映射
                if (bestSrcElements != null && bestSrcElements.contains(srcElement)) {
                    goodMappings.add(new ElementMapping(srcElement, dstElement));
                    srcElements.add(srcElement);
                    dstElements.add(dstElement);
                }
            }
        }

        if (goodMappings.size() == 0)
            return;
        //创建一个源元素列表，并按照源元素的位置进行排序
        List<ProgramElement> srcElementList = new ArrayList<>(srcElements);
        List<ProgramElement> dstElementList = new ArrayList<>(dstElements);
        srcElementList.sort(Comparator.comparingInt(ele -> ele.getITreeNode().getPos()));
        dstElementList.sort(Comparator.comparingInt(ele -> ele.getITreeNode().getPos()));

        getLcsEleToMap(srcElementList, dstElementList, goodMappings, elementsToMap);
    }

    /**
     * Process the multiple-to-multiple case with lcs.
     *
     * @param srcElements elements of source file in a pre-order
     * @param dstElements elements of target file in a pre-order
     * @param elementPair best mapped element pairs
     * @param elementsToMap  the element pairs that are calculated to be mapped
     */
    //通过最长公共子序列（LCS）算法，从两个给定的先序排列的src和dst列表中提取出相匹配的元素对，并将它们存储在elementsToMap中
    private static void getLcsEleToMap(List<ProgramElement> srcElements,
                                       List<ProgramElement> dstElements,
                                       Set<ElementMapping> elementPair,
                                       Map<ProgramElement, ProgramElement> elementsToMap){
        LongestCommonSubsequence<ProgramElement> lcs = new LongestCommonSubsequence<ProgramElement>(srcElements, dstElements) {
            @Override
            public boolean isEqual(ProgramElement t1, ProgramElement t2) {
                return elementPair.contains(new ElementMapping(t1, t2));
            }
        };
        List<int[]> idxes = lcs.extractIdxes();
        for (int[] idxPair: idxes){
            ProgramElement srcEle = srcElements.get(idxPair[0]);
            ProgramElement dstEle = dstElements.get(idxPair[1]);
//            System.out.println("LCS Ele To Map is " + srcEle + " " + dstEle);
            elementsToMap.put(srcEle, dstEle);
        }
    }
}
