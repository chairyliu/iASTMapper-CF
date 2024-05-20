package cs.model.algorithm.matcher.mappings;

import cs.model.algorithm.element.ProgramElement;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static cs.model.analysis.CrossFileRevisionAnalysis.compareTwo;

public class SelectCrossFileMapping {

    private ElementMappings eleMappings;
    private Map<ProgramElement, ProgramElement> srcToDstMap;
    private Map<ProgramElement, ProgramElement> dstToSrcMap;
    public final Map<Map<String, String>, String> crossFileMap = new HashMap<>();

    public SelectCrossFileMapping(ElementMappings eleMappings, String srcPath, Map<String, String> pathMap,
                                  Map<String, Set<ProgramElement>> allDstPathToStmtsMap,
                                  Map<String, Set<ProgramElement>> allDstPathToTokensMap,
                                  Map<String, Set<ProgramElement>> allDstPathToinnerStmtsMap,
                                  Map<String, Set<ProgramElement>> allSrcPathToStmtsMap) {
        srcToDstMap = eleMappings.getSrcToDstMap();
//        dstToSrcMap = eleMappings.getDstToSrcMap();
        Map<String, String> invertedPathMap = new HashMap<>();
        for (Map.Entry<String, String> entry : pathMap.entrySet()) {
            invertedPathMap.put(entry.getValue(), entry.getKey());
        }

        Set<ProgramElement> srcStmtsToMap = allSrcPathToStmtsMap.get(srcPath);
        String dstPath = pathMap.get(srcPath);
        Set<ProgramElement> samePathDstStmts = allDstPathToStmtsMap.get(dstPath);

        for (ProgramElement srcEle : srcStmtsToMap) {
            if (srcEle.getStringValue().equals(""))
                continue;
//            if (srcEle.getStringValue().startsWith("import") || srcEle.getStringValue().startsWith("package"))
//                continue;
//            if (srcEle.getStringValue().equals("pool close"))
//                System.out.println(srcEle);
            ProgramElement dstEle = srcToDstMap.get(srcEle);
            if (dstEle == null || dstEle.getStringValue().equals(""))
                continue;
//            String src = "syncChannel stop WAIT_FOREVER_TIMEOUT";
//            String dst = "next stop";
//            double sim = compareTwo(src, dst);
//            System.out.println(sim);
            double similarity = compareTwo(srcEle.getStringValue(), dstEle.getStringValue());
            if (similarity < 0.45)
                continue;
            if (samePathDstStmts != null && samePathDstStmts.contains(dstEle))
                continue;
            processDstElements(allDstPathToStmtsMap, allSrcPathToStmtsMap,invertedPathMap, srcPath, dstPath, srcEle, dstEle);
        }
    }

    private void processDstElements(Map<String, Set<ProgramElement>> allDstPathToStmtsMap, Map<String, Set<ProgramElement>> allSrcPathToStmtsMap,Map<String, String> invertedPathMap,
                                    String srcPath, String dstPath, ProgramElement srcEle, ProgramElement dstEle) {
        for (Map.Entry<String, Set<ProgramElement>> entry : allDstPathToStmtsMap.entrySet()) {
            String tmp_dstPath = entry.getKey();
            if (tmp_dstPath.equals(dstPath))
                continue;
            Set<ProgramElement> dstStmtSet = entry.getValue();
            for (ProgramElement dstStmt : dstStmtSet) {
                if (dstStmt.equals(dstEle)) {
                    String tmp_srcPath = invertedPathMap.get(tmp_dstPath);
                    Set<ProgramElement> srcStmtsSet = allSrcPathToStmtsMap.get(tmp_srcPath);
                    boolean isContinue = true;
                    for (ProgramElement tmp_srcStatement : srcStmtsSet) {
                        if (tmp_srcStatement.getStringValue().equals(dstEle.getStringValue())) {
                            isContinue = false;
                            break;
                        }
                    }
                    if (isContinue)
                        recordCrossFileMapping(srcPath, tmp_dstPath, srcEle, dstEle);
                }
            }
        }
    }

    private void recordCrossFileMapping(String srcPath, String tmp_dstPath, ProgramElement srcEle, ProgramElement dstEle) {
        Map<String, String> srcAndDstPathToSrcStmtMap = new HashMap<>();
        String path = srcPath + "+" + tmp_dstPath;
        String srcValue = srcEle.getStringValue();
        String dstValue = dstEle.getStringValue();
        srcAndDstPathToSrcStmtMap.put(path, srcValue);
        crossFileMap.put(srcAndDstPathToSrcStmtMap, dstValue);
    }

    public Map<Map<String, String>, String> getCrossFileMap(){
        return crossFileMap;
    }
}
