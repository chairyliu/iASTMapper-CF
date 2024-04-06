package cs.model.algorithm.matcher.matchers;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.matchers.searchers.BestMappingSearcher;
import cs.model.algorithm.matcher.rules.ElementMatchDeterminer;
import cs.model.algorithm.matcher.mappings.ElementMapping;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.evaluation.config.MyConfig;

import java.util.*;

/**
 * Base Matcher for statements, tokens and inner-stmt elements
 * that uses diverse similarity measures.
 *
 * Method:
 * We have two sets of elements from two files, respectively.
 * For each element in each set, we calculate a set of candidate elements.
 * From the candidate elements, we aim to find the best-mapped element.
 */
public abstract class BaseMatcher {
    // the searcher of best mappings
    protected final BestMappingSearcher bestMappingSearcher;

    // mappings of statements, tokens and inner-stmt elements
    protected final ElementMappings elementMappings;

    // source elements that are not mapped
    protected Set<ProgramElement> srcElementsToMap;

    // Whether to process tokens
    protected boolean processToken = false;

    protected Map<ElementMapping, Integer> mappingTimeRecord;//键是一对匹配的<srcEle,dstEle>,值是他们出现匹配的次数

    protected Set<ProgramElement> infiniteLoopElements;


    public String Matcher_type;

    private static int infiniteLoopTime = MyConfig.getInfiniteLoopTime();

    public BaseMatcher(ElementMappings elementMappings, BestMappingSearcher bestMappingSearcher){
        this.elementMappings = elementMappings;
        this.bestMappingSearcher = bestMappingSearcher;
        this.mappingTimeRecord = new HashMap<>();
        this.infiniteLoopElements = new HashSet<>();
    }
    public void setMatcher_type(String the_type){
        this.Matcher_type = the_type;
    }
    public void calSrcElementsToMap() {
        this.srcElementsToMap = getAllSrcElementsToMap();
    }
    public ElementMappings getElementMappings() {
        return elementMappings;
    }

    public ElementSimMeasures getElementSimMeasures(ProgramElement srcEle, ProgramElement dstEle){
        ElementSimMeasures measures = bestMappingSearcher.getElementSimMeasures(srcEle, dstEle);
        if (measures == null)
            measures = new ElementSimMeasures(srcEle, dstEle);
        return measures;
    }

    public void setProcessToken(boolean processToken) {
        this.processToken = processToken;
    }

    /**
     * Iteratively build mappings for stmt or token between
     * file before and after a revision
     */
    public boolean buildMappingsInnerLoop() {
        Map<ProgramElement, ProgramElement> elementsToMap = new HashMap<>();
        boolean findMapping = false;
        removeIllegalMappings();

        do {
            ElementMatchDeterminer determiner = new ElementMatchDeterminer(elementMappings);//构建相似度规则的判别器对象
            bestMappingSearcher.setElementMappings(elementMappings);//每次更新元素映射时设置当前元素映射
            bestMappingSearcher.setElementMatchDeterminer(determiner);
            bestMappingSearcher.clearBestCandidateMaps();

            // record if we find new mappings in this loop
            boolean findMappingInThisLoop = false;

            elementsToMap.clear();

            // First find mappings considering all the target elements
            bestMappingSearcher.findElementPairsToMap(elementsToMap, srcElementsToMap, false);//false代表全局
            if (elementsToMap.size() > 0) {//从全局中找到的一对一映射不为空
                // We first check if there may exist an infinite loop.
                // Then, we add the element pairs to element mappings
                for (ProgramElement srcEle : elementsToMap.keySet()) {
                    if (infiniteLoopElements.contains(srcEle))//检查是否出现了无限循环的匹配对（3次）
                        continue;
                    ProgramElement dstEle = elementsToMap.get(srcEle);
                    if (infiniteLoopElements.contains(dstEle))
                        continue;//如果<srcEle,dstEle>对均不是无限循环
                    addElementMapping(srcEle, dstEle);
                    findMappingInThisLoop = true;//表明在此次循环中有新的映射产生
                }
            } else {
                // find mappings excluding target elements that have been mapped.
                bestMappingSearcher.findElementPairsToMap(elementsToMap, srcElementsToMap, true);//true代表局部

                // add the element pairs to element mappings
                for (ProgramElement srcEle: elementsToMap.keySet()) {
                    ProgramElement dstEle = elementsToMap.get(srcEle);
                    addElementMapping(srcEle, dstEle);
                    findMappingInThisLoop = true;
                }
            }
            findMapping |= findMappingInThisLoop;//位运算符，只要findMappingInThisLoop中的任何一个值为true，findMapping为true
            if (!findMappingInThisLoop)
                break;
        } while (elementsToMap.size() > 0);

        return findMapping;
    }

    private void removeIllegalMappings() {//根据相似性度量的条件判断
        for (ProgramElement srcEle: srcElementsToMap) {///遍历还没有匹配的src元素
            if (elementMappings.isMapped(srcEle)) {
                ProgramElement dstEle = elementMappings.getMappedElement(srcEle);
                ElementSimMeasures measures = getElementSimMeasures(srcEle, dstEle);
                ElementMatchDeterminer determiner = new ElementMatchDeterminer(elementMappings);
                if (!determiner.determine(measures)) {
                    elementMappings.removeMapping(srcEle);
                }
            }
        }
    }

    /**
     * Add mapping between two elements
     * @param srcEle source element
     * @param dstEle destination element
     */
    //将传入的srcEle和dstEle的匹配次数+1，如果超过3，则加入到无限循环列表中，否则，添加到elementMappings中
    protected void addElementMapping(ProgramElement srcEle, ProgramElement dstEle){
        ElementMapping mapping = new ElementMapping(srcEle, dstEle);
        if (!mappingTimeRecord.containsKey(mapping))//如果已有的映射集合中没有当前映射，那么新加入，并且为匹配次数赋值为0
            mappingTimeRecord.put(mapping, 0);
        mappingTimeRecord.put(mapping, mappingTimeRecord.get(mapping) + 1);//如果当前映射已经存在，那么匹配次数+1
        if (mappingTimeRecord.get(mapping) >= infiniteLoopTime) {//判断匹配次数是否大于3，如果是，将当前src和dst元素添加到无限循环的集合中，之后遇到该元素，直接跳过
            infiniteLoopElements.add(srcEle);
            infiniteLoopElements.add(dstEle);
        }
        elementMappings.addMapping(srcEle, dstEle);//如果不是无限循环，那么将该匹配对加入elementMappings
    }

    protected abstract Set<ProgramElement> getAllSrcElementsToMap();
}
