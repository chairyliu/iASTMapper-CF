package cs.model.algorithm.matcher.matchers.searchers;

import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.ProgramElementType;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;

import java.util.*;

public class CandidateSetsAndMaps {//将快速阶段未匹配的三种元素筛选出来，并将这三种element与他们的type相对应，构成集合
    private final Set<ProgramElement> allSrcStmts;
    private final Set<ProgramElement> srcStmtsToMap; //在快速映射阶段没有映射的src中的stmt元素
    private final Set<ProgramElement> srcTokensToMap;//在快速映射阶段没有映射的src中的token元素
    private final Set<ProgramElement> srcinnerStmtsToMap;//在快速映射阶段没有映射的src中的inner-stmt元素
//    private final Set<ProgramElement> dstStmtsToMap;
//    private final Set<ProgramElement> dstTokensToMap;
//    private final Set<ProgramElement> dstinnerStmtsToMap;

    private final Map<ProgramElementType, Set<ProgramElement>> dstTypeElementMap;
    private final Map<String, Set<ProgramElement>> dstValTokenMap;//键是token的value，值是token元素，一个value值可能对应多个token元素
    private final Map<String, Set<ProgramElement>> dstValMultiTokenElementMap;//键是innerstmt的类型与value值的拼接，值是所有的innerstmt集合

    private final ElementMappings fastEleMappings;

    public CandidateSetsAndMaps(ElementMappings fastEleMappings, List<ProgramElement> srcStmts,
                                Set<ProgramElement> allDstStmts, List<ProgramElement> AllDstStmtsToMap,
                                List<ProgramElement> AllDstTokensToMap, List<ProgramElement> AllDstinnerStmtsToMap) {//全部的src和dst元素
        this.fastEleMappings = fastEleMappings;
        this.dstTypeElementMap = new HashMap<>();
        this.dstValTokenMap = new HashMap<>();
        this.dstValMultiTokenElementMap = new HashMap<>();
        this.allSrcStmts = new HashSet<>(srcStmts);
        this.srcStmtsToMap = new HashSet<>();
        this.srcTokensToMap = new HashSet<>();
        this.srcinnerStmtsToMap = new HashSet<>();
//        this.dstStmtsToMap = new HashSet<>();
//        this.dstTokensToMap = new HashSet<>();
//        this.dstinnerStmtsToMap = new HashSet<>();
        initStmtsAndTokens(srcStmts);
        initMultiTokenElementMap(allDstStmts);
        initMap(AllDstStmtsToMap, AllDstTokensToMap, AllDstinnerStmtsToMap);
    }

    private void initStmtsAndTokens(List<ProgramElement> srcStmts) {//将快速映射阶段没有映射的stmt、token、inner都存入各自的集合中，方便后续映射
        for (ProgramElement srcStmt : srcStmts) {
            if (!fastEleMappings.isMapped(srcStmt)) {
                this.srcStmtsToMap.add(srcStmt);//在快速映射阶段没有映射的语句存入srcStmtsToMap集合
            }
            for (ProgramElement tokenEle : srcStmt.getTokenElements()) {
                if (!fastEleMappings.isMapped(tokenEle))
                    this.srcTokensToMap.add(tokenEle);
            }
            for (ProgramElement innerStmtEle : srcStmt.getInnerStmtElements()) {
                if (!fastEleMappings.isMapped(innerStmtEle))
                    this.srcinnerStmtsToMap.add(innerStmtEle);
            }
        }
    }
//
//        for (ProgramElement dstStmt: dstStmts) {
//            if (!fastEleMappings.isMapped(dstStmt)) {
//                this.dstStmtsToMap.add(dstStmt);
//            }
//            for (ProgramElement tokenEle: dstStmt.getTokenElements()) {
//                if (!fastEleMappings.isMapped(tokenEle)) {
//                    this.dstTokensToMap.add(tokenEle);
//                    String value = tokenEle.getStringValue();
//                    if (!dstValTokenMap.containsKey(value))
//                        dstValTokenMap.put(value, new HashSet<>());
//                    dstValTokenMap.get(value).add(tokenEle);
//                }
//                for (ProgramElement innerStmtEle: ((TokenElement) tokenEle).getInnerStmtElementsWithToken()) {//为什么dst对应的token还对token的内部语句进行了检查，token还有内部语句？
//                    if (!fastEleMappings.isMapped(innerStmtEle))
//                        this.dstinnerStmtsToMap.add(innerStmtEle);
//                }
//            }
//            addrecursive(dstStmt.getInnerStmtElements());//传入当前遍历的这条dstStmt，对其下面的内部语句检查，如果在快速映射阶段没有被映射，则添加到对应的inner-stmt集合中
//        }
//    }
//    //递归，在快速映射阶段 语句的内部语句 没有映射的要被存入dstInnerStmtsToMap集合中，继续递归 内部语句的内部语句
//    private void addrecursive(List<InnerStmtElement> innerStmtElementList){
//        if(innerStmtElementList == null)
//            return;
//        for (ProgramElement innerStmtEle: innerStmtElementList) {
//            if (!fastEleMappings.isMapped(innerStmtEle))
//                this.dstinnerStmtsToMap.add(innerStmtEle);
//            addrecursive(innerStmtEle.getInnerStmtElements());
//        }
//    }
    //初始化token元素与其字符串值匹配的集合
    private void initMultiTokenElementMap(Set<ProgramElement> allDstStmts) {
        for (ProgramElement dstStmt: allDstStmts) {
            for (TokenElement tokenEle: dstStmt.getTokenElements()) {//获取语句元素的token集合
                if (fastEleMappings.isMapped(tokenEle))//如果token已经在快速映射阶段被映射了，结束
                    continue;
                List<InnerStmtElement> elements = tokenEle.getInnerStmtElementsWithToken();//获取token对应的内部语句元素
                for (InnerStmtElement element: elements) {
                    if (element.getTokenElements().size() == 1)//如果内部语句含有的token等于1，就视为该内部语句下没有token元素
                        continue;
                    String value = element.getStringValue();//得到内部语句的字符串值
                    String typeWithValue = element.getNodeType() + ":" + value;//将节点类型和字符串值拼接在一起
                    if (!dstValMultiTokenElementMap.containsKey(typeWithValue))//如果字符串值和多token元素对应的集合中不包含此typeWithValue
                        dstValMultiTokenElementMap.put(typeWithValue, new HashSet<>());
                    dstValMultiTokenElementMap.get(typeWithValue).add(element);//添加
                }
            }
        }
    }

    private void initMap(List<ProgramElement> AllDstStmtsToMap, List<ProgramElement> AllDstTokensToMap,
                         List<ProgramElement> AllDstinnerStmtsToMap) {
        // target value stmt map
        for (ProgramElement dstStmt: AllDstStmtsToMap)
            addElementTypeToMap(dstStmt, dstTypeElementMap);//将dststmt和其类型对应起来，map套map

        // target value token map
        for (ProgramElement dstToken: AllDstTokensToMap)
            addElementTypeToMap(dstToken, dstTypeElementMap);

        // target value inner stmt map
//        System.out.println("the size of the inner stmt is " + dstinnerStmtsToMap.size());
        for (ProgramElement dstInnerStmt: AllDstinnerStmtsToMap)
            addElementTypeToMap(dstInnerStmt, dstTypeElementMap);
    }

    private void addElementTypeToMap(ProgramElement element, Map<ProgramElementType, Set<ProgramElement>> typeEleMap) {
        ProgramElementType type = element.getElementType();//获取传入元素的类型
//        System.out.println("Type is " + type + " " + element);
        if (!typeEleMap.containsKey(type))
            typeEleMap.put(type, new HashSet<>());
        typeEleMap.get(type).add(element);//typeEleMap中的键指类型，值指的是是这个类型的所有元素集合
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

//    public Set<ProgramElement> getSameValDstCandidates(String value) {
//        Set<ProgramElement> ret = new HashSet<>();
//        if (dstValTokenMap.containsKey(value))
//            ret.addAll(dstValTokenMap.get(value));
//        return ret;
//    }

    public Set<ProgramElement> getSameValDstMultiTokenElements(String typeWithValue) {
        Set<ProgramElement> ret = new HashSet<>();
        if (dstValMultiTokenElementMap.containsKey(typeWithValue))
            ret.addAll(dstValMultiTokenElementMap.get(typeWithValue));
        return ret;
    }
}
