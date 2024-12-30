package cs.model.algorithm.matcher.matchers.searchers;

import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.ProgramElementType;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;

import java.util.*;

public class CandidateSetsAndMaps {
    private final Set<ProgramElement> allSrcStmts;
    private final Set<ProgramElement> srcStmtsToMap;
    private final Set<ProgramElement> srcTokensToMap;
    private final Set<ProgramElement> srcinnerStmtsToMap;
    private Set<ProgramElement> dstStmtsToMap;
    private Set<ProgramElement> dstTokensToMap;
    private Set<ProgramElement> dstinnerStmtsToMap;
    private Map<String, Set<ProgramElement>> dstValTokenMap;

    private final Map<ProgramElementType, Set<ProgramElement>> dstTypeElementMap;
    private final Map<String, Set<ProgramElement>> dstValMultiTokenElementMap;
    private final ElementMappings fastEleMappings;
    public static Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap;
    public Map<String, Set<ProgramElement>> srcPathToStmtsMap;
    public Map<String, Set<ProgramElement>> AllDstValTokenMap;
    public boolean doCrossFileMapping;

    public CandidateSetsAndMaps(ElementMappings fastEleMappings, List<ProgramElement> srcStmts,
                                List<ProgramElement> allDstStmts, List<ProgramElement> AllDstStmtsList,
                                List<ProgramElement> AllDstTokensList, List<ProgramElement> AllDstinnerStmtsList,
                                Map<String, Set<ProgramElement>> AllDstValTokenMap,
                                Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap, boolean doCrossFileMapping) {
        this.fastEleMappings = fastEleMappings;
        this.dstTypeElementMap = new HashMap<>();
        this.dstValMultiTokenElementMap = new HashMap<>();
        this.srcStmtsToMap = new HashSet<>();
        this.srcTokensToMap = new HashSet<>();
        this.srcinnerStmtsToMap = new HashSet<>();
        this.AllSrcPathToStmtsMap = AllSrcPathToStmtsMap;
        this.srcPathToStmtsMap = new HashMap<>();
        this.allSrcStmts = new HashSet<>(srcStmts);
        this.AllDstValTokenMap = AllDstValTokenMap;
        initSrcOrDstELements(srcStmts, allDstStmts, this.doCrossFileMapping);
        initMultiTokenElementMap(allDstStmts);
        initAllDstELeMap(AllDstStmtsList, AllDstTokensList, AllDstinnerStmtsList);
    }

    public CandidateSetsAndMaps(ElementMappings fastEleMappings, List<ProgramElement> srcStmts,
                                List<ProgramElement> dstStmts, boolean doCrossFileMapping) {
        this.fastEleMappings = fastEleMappings;
        this.dstTypeElementMap = new HashMap<>();
        this.dstValMultiTokenElementMap = new HashMap<>();
        this.allSrcStmts = new HashSet<>(srcStmts);
        this.srcStmtsToMap = new HashSet<>();
        this.srcTokensToMap = new HashSet<>();
        this.srcinnerStmtsToMap = new HashSet<>();
        initSrcOrDstELements(srcStmts, dstStmts, this.doCrossFileMapping);
        initMultiTokenElementMap(dstStmts);
        initMap();
    }

    /**
     *Initialize collections for statements, tokens, and inner statements that were not mapped during the fast mapping phase.
     * @param srcStmts
     */
    private void initSrcOrDstELements(List<ProgramElement> srcStmts, List<ProgramElement> dstStmts, boolean doCrossFileMapping) {
        this.dstStmtsToMap = new HashSet<>();
        this.dstTokensToMap = new HashSet<>();
        this.dstinnerStmtsToMap = new HashSet<>();
        this.dstValTokenMap = new HashMap<>();
        for (ProgramElement srcStmt: srcStmts) {
            if (!fastEleMappings.isMapped(srcStmt)) {
                this.srcStmtsToMap.add(srcStmt);
            }
            for (ProgramElement tokenEle: srcStmt.getTokenElements()) {
                if (!fastEleMappings.isMapped(tokenEle))
                    this.srcTokensToMap.add(tokenEle);
            }
            for (ProgramElement innerStmtEle: srcStmt.getInnerStmtElements()) {
                if (!fastEleMappings.isMapped(innerStmtEle))
                    this.srcinnerStmtsToMap.add(innerStmtEle);
            }
        }

        if (!doCrossFileMapping) {
            for (ProgramElement dstStmt: dstStmts) {
                if (!fastEleMappings.isMapped(dstStmt)) {
                    this.dstStmtsToMap.add(dstStmt);
                }
                for (ProgramElement tokenEle: dstStmt.getTokenElements()) {
                    if (!fastEleMappings.isMapped(tokenEle)) {
                        this.dstTokensToMap.add(tokenEle);
                        String value = tokenEle.getStringValue();
                        if (!dstValTokenMap.containsKey(value))
                            dstValTokenMap.put(value, new HashSet<>());
                        dstValTokenMap.get(value).add(tokenEle);
                    }
                    for (ProgramElement innerStmtEle: ((TokenElement) tokenEle).getInnerStmtElementsWithToken()) {
                        if (!fastEleMappings.isMapped(innerStmtEle))
                            this.dstinnerStmtsToMap.add(innerStmtEle);
                    }
                }
                addrecursive(dstStmt.getInnerStmtElements());
            }
        }
    }

    private void addrecursive(List<InnerStmtElement> innerStmtElementList){
        if(innerStmtElementList == null)
            return;
        for (ProgramElement innerStmtEle: innerStmtElementList) {
            if (!fastEleMappings.isMapped(innerStmtEle))
                this.dstinnerStmtsToMap.add(innerStmtEle);
            addrecursive(innerStmtEle.getInnerStmtElements());
        }
    }

    /**
     * Initialize the multi-token element map for destination statements with multiple tokens.
     * @param allDstStmts
     */
    private void initMultiTokenElementMap(List<ProgramElement> allDstStmts) {
        for (ProgramElement dstStmt: allDstStmts) {
            for (TokenElement tokenEle: dstStmt.getTokenElements()) {
                if (fastEleMappings.isMapped(tokenEle))
                    continue;
                List<InnerStmtElement> elements = tokenEle.getInnerStmtElementsWithToken();
                for (InnerStmtElement element: elements) {
                    if (element.getTokenElements().size() == 1)
                        continue;
                    String value = element.getStringValue();
                    String typeWithValue = element.getNodeType() + ":" + value;
                    if (!dstValMultiTokenElementMap.containsKey(typeWithValue))
                        dstValMultiTokenElementMap.put(typeWithValue, new HashSet<>());
                    dstValMultiTokenElementMap.get(typeWithValue).add(element);
                }
            }
        }
    }

    private void initAllDstELeMap(List<ProgramElement> AllDstStmtsList, List<ProgramElement> AllDstTokensList,
                                  List<ProgramElement> AllDstinnerStmtsList) {
        // target value stmt map
        for (ProgramElement dstStmt: AllDstStmtsList)
            addElementTypeToMap(dstStmt, dstTypeElementMap);

        // target value token map
        for (ProgramElement dstToken: AllDstTokensList)
            addElementTypeToMap(dstToken, dstTypeElementMap);

        // target value inner stmt map
        for (ProgramElement dstInnerStmt: AllDstinnerStmtsList)
            addElementTypeToMap(dstInnerStmt, dstTypeElementMap);
    }

    private void initMap() {
        // target value stmt map
        for (ProgramElement dstStmt: dstStmtsToMap)
            addElementTypeToMap(dstStmt, dstTypeElementMap);

        // target value token map
        for (ProgramElement dstToken: dstTokensToMap)
            addElementTypeToMap(dstToken, dstTypeElementMap);

        // target value inner stmt map
        for (ProgramElement dstInnerStmt: dstinnerStmtsToMap)
            addElementTypeToMap(dstInnerStmt, dstTypeElementMap);
    }

    private void addElementTypeToMap(ProgramElement element, Map<ProgramElementType, Set<ProgramElement>> typeEleMap) {
        ProgramElementType type = element.getElementType();
        if (!typeEleMap.containsKey(type))
            typeEleMap.put(type, new HashSet<>());
        typeEleMap.get(type).add(element);
    }

    public Set<ProgramElement> getSameValDstCandidates(String value) {
        Set<ProgramElement> ret = new HashSet<>();
        if (doCrossFileMapping){
            if (this.AllDstValTokenMap.containsKey(value))
                ret.addAll(this.AllDstValTokenMap.get(value));
        } else {
            if (dstValTokenMap.containsKey(value))
                ret.addAll(dstValTokenMap.get(value));
        }
        return ret;
    }

    public Set<ProgramElement> getAllSrcStmts() {
        return allSrcStmts;
    }

    public Set<ProgramElement> getSrcStmtsToMap() {
        return srcStmtsToMap;
    }

    public Set<ProgramElement> getSrcTokensToMap() {
        return srcTokensToMap;
    }

    public Map<ProgramElementType, Set<ProgramElement>> getDstTypeElementMap() {
        return dstTypeElementMap;
    }

    public Set<ProgramElement> getSameTypeDstCandidates(ProgramElementType type) {
        Set<ProgramElement> ret = new HashSet<>();
        if (dstTypeElementMap.containsKey(type))
            ret.addAll(dstTypeElementMap.get(type));
        return ret;
    }

    public Set<ProgramElement> getSameValDstMultiTokenElements(String typeWithValue) {
        Set<ProgramElement> ret = new HashSet<>();
        if (dstValMultiTokenElementMap.containsKey(typeWithValue))
            ret.addAll(dstValMultiTokenElementMap.get(typeWithValue));
        return ret;
    }
}
