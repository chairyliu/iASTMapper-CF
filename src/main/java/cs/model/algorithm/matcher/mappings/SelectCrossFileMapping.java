package cs.model.algorithm.matcher.mappings;

import cs.model.algorithm.element.ProgramElement;

import java.util.*;

public class SelectCrossFileMapping {

    private ElementMappings eleMappings;
    private Map<ProgramElement, ProgramElement> srcToDstMap;
    private Map<ProgramElement, ProgramElement> dstToSrcMap;
    public final Map<Map<String, String>, String> crossFileMap = new HashMap<>();

    public SelectCrossFileMapping(ElementMappings eleMappings, String srcPath,
                                  Map<Set<ProgramElement>, String> allDstPathToStmtsMap,
                                  Map<Set<ProgramElement>, String> allDstPathToTokensMap,
                                  Map<Set<ProgramElement>, String> allDstPathToinnerStmtsMap){
        srcToDstMap = eleMappings.getSrcToDstMap();
        dstToSrcMap = eleMappings.getDstToSrcMap();
//        System.out.println(dstToSrcMap);
//        Map<String, String> srcAndDstPathToSrcStmtMap = new HashMap<>();
//        List<ProgramElement> tmp_crossFileDstStmts = new ArrayList<>();
        for (ProgramElement srcEle : srcToDstMap.keySet()){
            if (srcEle.isStmt()){
                ProgramElement dstEle = srcToDstMap.get(srcEle);
                for (Set<ProgramElement> dstStmtSet : allDstPathToStmtsMap.keySet()){
                    String dstPath = allDstPathToStmtsMap.get(dstStmtSet);
                    if (dstPath.equals(srcPath)){
                        continue;
                    }
                    for (ProgramElement dstStmt : dstStmtSet){
                        if (dstEle.equals(dstStmt)) {
//                            srcToDstMap.remove(srcEle);
//                            dstToSrcMap.remove(dstStmt);
                            Map<String, String> srcAndDstPathToSrcStmtMap = new HashMap<>();
                            String path = srcPath + "+" + dstPath;
                            String srcValue = srcEle.getStringValue();
                            String dstValue = dstEle.getStringValue();
//                            System.out.println(dstValue);
                            srcAndDstPathToSrcStmtMap.put(path, srcValue);
                            crossFileMap.put(srcAndDstPathToSrcStmtMap, dstValue);
//                            System.out.println(crossFileMap);
                        }
//                        srcAndDstPathToSrcStmtMap.clear();
                    }
                }
            }
        }
    }

    public Map<Map<String, String>, String> getCrossFileMap(){
        return crossFileMap;
    }
}
