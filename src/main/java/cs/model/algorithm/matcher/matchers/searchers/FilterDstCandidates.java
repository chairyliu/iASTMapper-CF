package cs.model.algorithm.matcher.matchers.searchers;

import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;

import java.util.*;

public class FilterDstCandidates {
    private final ElementMappings fastEleMappings;
    private final Set<ProgramElement> dstStmtsToMap;
    private final Set<ProgramElement> dstTokensToMap;
    private final Set<ProgramElement> dstinnerStmtsToMap;
    private final Map<String, Set<ProgramElement>> dstValTokenMap;
    public Map<String, Set<ProgramElement>> dstPathToStmtsMap;
    public Map<String, Set<ProgramElement>> dstPathToTokensMap;
    public Map<String, Set<ProgramElement>> dstPathToinnerStmtsMap;
    public Map<String, Set<ProgramElement>> AllDstPathToStmtsMap;
    public List<ProgramElement> AllDstStmtsList;
    public List<ProgramElement> AllDstTokensList;
    public List<ProgramElement> AllDstinnerStmtsList;
    public Map<String, Set<ProgramElement>> AllDstValTokenMap;
    public Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap;
    private final Set<ProgramElement> SrcStmtsToMap;
    public Map<String, Set<ProgramElement>> srcPathToStmtsMap;
    public FilterDstCandidates(ElementMappings fastEleMappings, List<ProgramElement> AllDstStmtsList,
                               List<ProgramElement> AllDstTokensList, List<ProgramElement> AllDstinnerStmtsList,
                               Map<String, Set<ProgramElement>> AllDstPathToStmtsMap,
                               Map<String, Set<ProgramElement>> AllDstValTokenMap,
                               Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap){
        this.fastEleMappings = fastEleMappings;
        this.dstStmtsToMap = new HashSet<>();
        this.dstTokensToMap = new HashSet<>();
        this.dstinnerStmtsToMap = new HashSet<>();
        this.dstValTokenMap = new HashMap<>();
        this.dstPathToStmtsMap = new HashMap<>();
        this.dstPathToTokensMap = new HashMap<>();
        this.dstPathToinnerStmtsMap = new HashMap<>();
        this.SrcStmtsToMap = new HashSet<>();
        this.srcPathToStmtsMap = new HashMap<>();
        this.AllDstStmtsList = AllDstStmtsList;
        this.AllDstTokensList = AllDstTokensList;
        this.AllDstinnerStmtsList = AllDstinnerStmtsList;
        this.AllDstPathToStmtsMap = AllDstPathToStmtsMap;
        this.AllDstValTokenMap = AllDstValTokenMap;
        this.AllSrcPathToStmtsMap = AllSrcPathToStmtsMap;
    }

    /**
     * Initialize collections for statements, tokens, and inner statements that were not mapped during the fast mapping phase.
     * Store all target elements and their corresponding paths from the same commit into the respective collections.
     */
    public void initDstElements(List<ProgramElement> srcStmts, List<ProgramElement> dstStmts, String srcPath,
                                String dstPath, List<ProgramElement> AllDstStmtsList,
                                List<ProgramElement> AllDstTokensList, List<ProgramElement> AllDstinnerStmtsList,
                                Map<String, Set<ProgramElement>> AllDstPathToStmtsMap,
                                Map<String, Set<ProgramElement>> AllDstValTokenMap, Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap) {
        for (ProgramElement srcStmt: srcStmts) {
            if (!fastEleMappings.isMapped(srcStmt)) {
                this.SrcStmtsToMap.add(srcStmt);
            }
        }
        srcPathToStmtsMap.put(srcPath, this.SrcStmtsToMap);
        AllSrcPathToStmtsMap.putAll(srcPathToStmtsMap);

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
                    if (!fastEleMappings.isMapped(innerStmtEle)){
                        this.dstinnerStmtsToMap.add(innerStmtEle);
                    }
                }
            }
            addrecursiveInnerStmt(dstStmt.getInnerStmtElements());
        }
        dstPathToStmtsMap.put(dstPath, dstStmtsToMap);
        dstPathToTokensMap.put(dstPath, dstTokensToMap);
        dstPathToinnerStmtsMap.put(dstPath, dstinnerStmtsToMap);
        AllDstPathToStmtsMap.putAll(dstPathToStmtsMap);
        AllDstStmtsList.addAll(this.dstStmtsToMap);
        AllDstTokensList.addAll(this.dstTokensToMap);
        AllDstinnerStmtsList.addAll(this.dstinnerStmtsToMap);
        AllDstValTokenMap.putAll(dstValTokenMap);
    }

    private void addrecursiveInnerStmt(List<InnerStmtElement> innerStmtElementList){
        if(innerStmtElementList == null)
            return;
        for (ProgramElement innerStmtEle: innerStmtElementList) {
            if (!fastEleMappings.isMapped(innerStmtEle))
                this.dstinnerStmtsToMap.add(innerStmtEle);
            addrecursiveInnerStmt(innerStmtEle.getInnerStmtElements());
        }
    }

    public Map<String, Set<ProgramElement>> getAllSrcPathToStmtsMap() {
        return AllSrcPathToStmtsMap;
    }
    public Map<String, Set<ProgramElement>> getAllDstPathToStmtsMap(){ return AllDstPathToStmtsMap; }
    public List<ProgramElement> getAllDstStmtsList() { return AllDstStmtsList; }
    public List<ProgramElement> getAllDstTokensList(){ return AllDstTokensList; }
    public List<ProgramElement> getAllDstinnerStmtsList(){ return AllDstinnerStmtsList; }
    public Map<String, Set<ProgramElement>> getAllDstValTokenMap() { return AllDstValTokenMap; }
}
