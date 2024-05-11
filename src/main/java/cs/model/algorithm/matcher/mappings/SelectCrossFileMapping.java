package cs.model.algorithm.matcher.mappings;

import cs.model.algorithm.element.ProgramElement;

import java.util.*;

public class SelectCrossFileMapping {

    private ElementMappings eleMappings;
    private Map<ProgramElement, ProgramElement> srcToDstMap;
    private Map<ProgramElement, ProgramElement> dstToSrcMap;
    public final Map<Map<String, String>, String> crossFileMap = new HashMap<>();

    public SelectCrossFileMapping(ElementMappings eleMappings, String srcPath, Map<String, String> pathMap,
                                  Map<String, Set<ProgramElement>> allDstPathToStmtsMap,
                                  Map<String, Set<ProgramElement>> allDstPathToTokensMap,
                                  Map<String, Set<ProgramElement>> allDstPathToinnerStmtsMap){
        srcToDstMap = eleMappings.getSrcToDstMap();
        dstToSrcMap = eleMappings.getDstToSrcMap();
//        System.out.println(dstToSrcMap);
//        Map<String, String> srcAndDstPathToSrcStmtMap = new HashMap<>();
        for (ProgramElement srcEle : srcToDstMap.keySet()){
            if (srcEle.isStmt() && !srcEle.getStringValue().equals("")){
//                String src = srcEle.getStringValue();
//                if (src.equals("package org activeio net")){
//                    ProgramElement dst = srcToDstMap.get(srcEle);
//                    System.out.println(dst);
//                }
                ProgramElement dstEle = srcToDstMap.get(srcEle);
//                System.out.println(dstEle);
                if (dstEle.getStringValue().equals(""))
                    continue;
                String dstPath = pathMap.get(srcPath);
                Set<ProgramElement> samePathDstStmts = allDstPathToStmtsMap.get(dstPath);
                boolean isCrossFileMapping = true;
                if (samePathDstStmts != null){
                    for (ProgramElement dstStmt : samePathDstStmts){
                        if (dstStmt.equals(dstEle)) {
                            isCrossFileMapping = false;
                            break;
                        }
                    }
                }
                if (isCrossFileMapping){
                    for (String everyDstPath : allDstPathToStmtsMap.keySet()){
                        if (everyDstPath.equals(dstPath))
                            continue;
                        Set<ProgramElement> dstStmtSet = allDstPathToStmtsMap.get(everyDstPath);
                        for (ProgramElement dstStmt : dstStmtSet){
                            if (dstEle.equals(dstStmt)) {
//                            srcToDstMap.remove(srcEle);
//                            dstToSrcMap.remove(dstStmt);
                                Map<String, String> srcAndDstPathToSrcStmtMap = new HashMap<>();
                                String path = srcPath + "+" + everyDstPath;
                                String srcValue = srcEle.getStringValue();
                                String dstValue = dstEle.getStringValue();
                                srcAndDstPathToSrcStmtMap.put(path, srcValue);
                                crossFileMap.put(srcAndDstPathToSrcStmtMap, dstValue);
//                                System.out.println(crossFileMap);
                            }
                        }
                    }
                }
            }
        }
    }

    public Map<Map<String, String>, String> getCrossFileMap(){
        return crossFileMap;
    }
}
