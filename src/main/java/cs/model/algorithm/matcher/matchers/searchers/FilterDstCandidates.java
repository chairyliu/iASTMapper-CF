package cs.model.algorithm.matcher.matchers.searchers;

import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;

import java.util.*;

public class FilterDstCandidates {
    private final ElementMappings fastEleMappings;
//    private final Set<ProgramElement> srcStmtsToMap; //在快速映射阶段没有映射的src中的stmt元素
//    private final Set<ProgramElement> srcTokensToMap;//在快速映射阶段没有映射的src中的token元素
//    private final Set<ProgramElement> srcinnerStmtsToMap;//在快速映射阶段没有映射的src中的inner-stmt元素
    private final Set<ProgramElement> dstStmtsToMap;
    private final Set<ProgramElement> dstTokensToMap;
    private final Set<ProgramElement> dstinnerStmtsToMap;
    private final Map<String, Set<ProgramElement>> dstValTokenMap;//键是token的value，值是token元素，一个value值可能对应多个token元素
//    public Map<String, Set<ProgramElement>> srcPathToStmtsMap;
//    public Map<String, Set<ProgramElement>> srcPathToTokensMap;
//    public Map<String, Set<ProgramElement>> srcPathToinnerStmtsMap;
    public Map<String, Set<ProgramElement>> dstPathToStmtsMap;
    public Map<String, Set<ProgramElement>> dstPathToTokensMap;
    public Map<String, Set<ProgramElement>> dstPathToinnerStmtsMap;
    public Map<String, Set<ProgramElement>> AllDstPathToStmtsMap;
    public Map<String, Set<ProgramElement>> AllDstPathToTokensMap;
    public Map<String, Set<ProgramElement>> AllDstPathToinnerStmtsMap;
    public List<ProgramElement> AllDstStmtsToMap;
    public List<ProgramElement> AllDstTokensToMap;
    public List<ProgramElement> AllDstinnerStmtsToMap;
    public Map<String, Set<ProgramElement>> AllDstValTokenMap;
    public Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap;
    private final Set<ProgramElement> SrcStmtsToMap;
    public Map<String, Set<ProgramElement>> srcPathToStmtsMap;
    public FilterDstCandidates(ElementMappings fastEleMappings, List<ProgramElement> srcStmts,
                               List<ProgramElement> dstStmts, String srcPath, String dstPath, List<ProgramElement> AllDstStmtsToMap,
                               List<ProgramElement> AllDstTokensToMap, List<ProgramElement> AllDstinnerStmtsToMap,
                               Map<String, Set<ProgramElement>> AllDstPathToStmtsMap,
                               Map<String, Set<ProgramElement>> AllDstPathToTokensMap,
                               Map<String, Set<ProgramElement>> AllDstPathToinnerStmtsMap,
                               Map<String, Set<ProgramElement>> AllDstValTokenMap,
                               Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap){
        this.fastEleMappings = fastEleMappings;
//        this.srcStmtsToMap = new HashSet<>();
//        this.srcTokensToMap = new HashSet<>();
//        this.srcinnerStmtsToMap = new HashSet<>();
        this.dstStmtsToMap = new HashSet<>();
        this.dstTokensToMap = new HashSet<>();
        this.dstinnerStmtsToMap = new HashSet<>();
        this.dstValTokenMap = new HashMap<>();
//        this.srcPathToStmtsMap = new HashMap<>();
//        this.srcPathToTokensMap = new HashMap<>();
//        this.srcPathToinnerStmtsMap = new HashMap<>();
        this.dstPathToStmtsMap = new HashMap<>();
        this.dstPathToTokensMap = new HashMap<>();
        this.dstPathToinnerStmtsMap = new HashMap<>();

        this.SrcStmtsToMap = new HashSet<>();
        this.srcPathToStmtsMap = new HashMap<>();

        this.AllDstStmtsToMap = AllDstStmtsToMap;
        this.AllDstTokensToMap = AllDstTokensToMap;
        this.AllDstinnerStmtsToMap = AllDstinnerStmtsToMap;
        this.AllDstPathToStmtsMap = AllDstPathToStmtsMap;
        this.AllDstPathToTokensMap = AllDstPathToTokensMap;
        this.AllDstPathToinnerStmtsMap = AllDstPathToinnerStmtsMap;
        this.AllDstValTokenMap = AllDstValTokenMap;
        this.AllSrcPathToStmtsMap = AllSrcPathToStmtsMap;
//        initStmtsAndTokens(srcStmts, dstStmts, srcPath, dstPath,AllDstStmtsToMap, AllDstTokensToMap,
//                AllDstinnerStmtsToMap,AllDstPathToStmtsMap, AllDstPathToTokensMap, AllDstPathToinnerStmtsMap);
    }

    public void initStmtsAndTokens(List<ProgramElement> srcStmts, List<ProgramElement> dstStmts, String srcPath,
                                   String dstPath, List<ProgramElement> AllDstStmtsToMap,
                                   List<ProgramElement> AllDstTokensToMap, List<ProgramElement> AllDstinnerStmtsToMap,
                                   Map<String, Set<ProgramElement>> AllDstPathToStmtsMap,
                                   Map<String, Set<ProgramElement>> AllDstPathToTokensMap,
                                   Map<String, Set<ProgramElement>> AllDstPathToinnerStmtsMap,
                                   Map<String, Set<ProgramElement>> AllDstValTokenMap,Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap) {//将快速映射阶段没有映射的stmt、token、inner都存入各自的集合中，方便后续映射
        for (ProgramElement srcStmt: srcStmts) {
            if (!fastEleMappings.isMapped(srcStmt)) {
                this.SrcStmtsToMap.add(srcStmt);//在快速映射阶段没有映射的语句存入srcStmtsToMap集合
            }
        }
        srcPathToStmtsMap.put(srcPath, this.SrcStmtsToMap);
        AllSrcPathToStmtsMap.putAll(srcPathToStmtsMap);
//            for (ProgramElement tokenEle: srcStmt.getTokenElements()) {
//                if (!fastEleMappings.isMapped(tokenEle))
//                    this.srcTokensToMap.add(tokenEle);
//            }
//            for (ProgramElement innerStmtEle: srcStmt.getInnerStmtElements()) {
//                if (!fastEleMappings.isMapped(innerStmtEle))
//                    this.srcinnerStmtsToMap.add(innerStmtEle);
//            }
//        }

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
                for (ProgramElement innerStmtEle: ((TokenElement) tokenEle).getInnerStmtElementsWithToken()) {//为什么dst对应的token还对token的内部语句进行了检查，token还有内部语句？
                    if (!fastEleMappings.isMapped(innerStmtEle))
                        this.dstinnerStmtsToMap.add(innerStmtEle);
                }
            }
            addrecursive(dstStmt.getInnerStmtElements());//传入当前遍历的这条dstStmt，对其下面的内部语句检查，如果在快速映射阶段没有被映射，则添加到对应的inner-stmt集合中
        }
//        srcPathToStmtsMap.put(srcPath, srcStmtsToMap);
//        srcPathToTokensMap.put(srcPath, srcTokensToMap);
//        srcPathToinnerStmtsMap.put(srcPath, srcinnerStmtsToMap);
        dstPathToStmtsMap.put(dstPath, dstStmtsToMap);
        dstPathToTokensMap.put(dstPath, dstTokensToMap);
        dstPathToinnerStmtsMap.put(dstPath, dstinnerStmtsToMap);
        AllDstPathToStmtsMap.putAll(dstPathToStmtsMap);
//        System.out.println(AllDstPathToStmtsMap);
        AllDstPathToTokensMap.putAll(dstPathToTokensMap);
        AllDstPathToinnerStmtsMap.putAll(dstPathToinnerStmtsMap);
        AllDstStmtsToMap.addAll(this.dstStmtsToMap);
//        System.out.println(AllDstStmtsToMap);
        AllDstTokensToMap.addAll(this.dstTokensToMap);
        AllDstinnerStmtsToMap.addAll(this.dstinnerStmtsToMap);
        AllDstValTokenMap.putAll(dstValTokenMap);
    }
    //递归，在快速映射阶段 语句的内部语句 没有映射的要被存入dstInnerStmtsToMap集合中，继续递归 内部语句的内部语句
    private void addrecursive(List<InnerStmtElement> innerStmtElementList){
        if(innerStmtElementList == null)
            return;
        for (ProgramElement innerStmtEle: innerStmtElementList) {
            if (!fastEleMappings.isMapped(innerStmtEle))
                this.dstinnerStmtsToMap.add(innerStmtEle);
            addrecursive(innerStmtEle.getInnerStmtElements());
        }
    }

    public Set<ProgramElement> getSameValDstCandidates(String value) {
        Set<ProgramElement> ret = new HashSet<>();
//        System.out.println(this.AllDstValTokenMap);
        if (this.AllDstValTokenMap.containsKey(value)){
//            System.out.println(this.AllDstValTokenMap);
            ret.addAll(this.AllDstValTokenMap.get(value));
        }
        return ret;
    }
//    public Map<String, Set<ProgramElement>> getSrcPathToStmtsMap(){ return srcPathToStmtsMap; }
//    public Map<String, Set<ProgramElement>> getSrcPathToTokensMap(){ return srcPathToTokensMap; }
//    public Map<String, Set<ProgramElement>> getSrcPathToinnerStmtsMap(){ return srcPathToinnerStmtsMap; }
    public Map<String, Set<ProgramElement>> getAllSrcPathToStmtsMap() {
        return AllSrcPathToStmtsMap;
    }
    public Map<String, Set<ProgramElement>> getDstPathToStmtsMap(){ return dstPathToStmtsMap; }
    public Map<String, Set<ProgramElement>> getDstPathToTokensMap(){ return dstPathToTokensMap; }
    public Map<String, Set<ProgramElement>> getDstPathToinnerStmtsMap(){ return dstPathToinnerStmtsMap; }
//    public Set<ProgramElement> getSrcStmtsToMap() { return srcStmtsToMap; }
//    public Set<ProgramElement> getSrcTokensToMap() {
//        return srcTokensToMap;
//    }
//    public Set<ProgramElement> getSrcinnerStmtsToMap() { return srcinnerStmtsToMap; }
//    public Set<ProgramElement> getDstStmtsToMap() { return dstStmtsToMap; }
//    public Set<ProgramElement> getDstTokensToMap() { return dstTokensToMap; }
//    public Set<ProgramElement> getDstinnerStmtsToMap() { return dstinnerStmtsToMap; }
    public Map<String, Set<ProgramElement>> getAllDstPathToStmtsMap(){ return AllDstPathToStmtsMap; }
    public Map<String, Set<ProgramElement>> getAllDstPathToTokensMap(){ return AllDstPathToTokensMap; }
    public Map<String, Set<ProgramElement>> getAllDstPathToinnerStmtsMap(){ return AllDstPathToinnerStmtsMap; }
    public List<ProgramElement> getAllDstStmtsToMap() { return AllDstStmtsToMap; }
    public List<ProgramElement> getAllDstTokensToMap(){ return AllDstTokensToMap; }
    public List<ProgramElement> getAllDstinnerStmtsToMap(){ return AllDstinnerStmtsToMap; }
    public Map<String, Set<ProgramElement>> getAllDstValTokenMap() { return AllDstValTokenMap; }
}
