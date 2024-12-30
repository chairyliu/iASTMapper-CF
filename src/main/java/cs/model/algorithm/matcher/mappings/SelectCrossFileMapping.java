package cs.model.algorithm.matcher.mappings;

import cs.model.algorithm.element.ProgramElement;

import java.util.*;

import static cs.model.analysis.CrossFileRevisionAnalysis.compareTwo;

/**
 * 1. For each source element, check which target element it matches.
 * 2. Ignore matching pairs with a similarity less than 0.75.
 * 3. If matching pairs come from different source and target files, check if the mapped target statements have a mapping in their own source files.
 *    If not, consider it a cross-file mapping.
 * 4. If a source statement has multiple cross-file mappings, consider it a one-to-many mapping and record it.
 */
public class SelectCrossFileMapping {

    private ElementMappings eleMappings;
    private Map<ProgramElement, ProgramElement> srcToDstMap;
    public final Map<Map<String, String>, String> crossFileMap = new HashMap<>();
    public final Map<String, List<String>> oneToMultiMap = new HashMap<>();

    public SelectCrossFileMapping(ElementMappings eleMappings, String srcPath, Map<String, String> pathMap,
                                  Map<String, Set<ProgramElement>> allDstPathToStmtsMap,
                                  Map<String, Set<ProgramElement>> allSrcPathToStmtsMap) {
        srcToDstMap = eleMappings.getSrcToDstMap();
        Map<String, String> invertedPathMap = new HashMap<>();
        for (Map.Entry<String, String> entry : pathMap.entrySet()) {
            invertedPathMap.put(entry.getValue(), entry.getKey());
        }

        Set<ProgramElement> srcStmts = allSrcPathToStmtsMap.get(srcPath);
        String dstPath = pathMap.get(srcPath);
        Set<ProgramElement> samePathDstStmts = allDstPathToStmtsMap.get(dstPath);

        for (ProgramElement srcEle : srcStmts) {
            if (srcEle.getStringValue().equals(""))
                continue;
//            if (srcEle.getStringValue().startsWith("import") || srcEle.getStringValue().startsWith("package"))
//                continue;
            ProgramElement dstEle = srcToDstMap.get(srcEle);
            if (dstEle == null || dstEle.getStringValue().equals(""))
                continue;
            double similarity = compareTwo(srcEle.getStringValue(), dstEle.getStringValue());
            if (similarity < 0.75)
                continue;
            if (samePathDstStmts != null && samePathDstStmts.contains(dstEle))
                continue;
            int oneToMultiMapping = 0;
            processDstElements(allDstPathToStmtsMap, allSrcPathToStmtsMap,invertedPathMap, srcPath, dstPath, srcEle, dstEle, oneToMultiMapping);
        }
    }

    private void processDstElements(Map<String, Set<ProgramElement>> allDstPathToStmtsMap, Map<String, Set<ProgramElement>> allSrcPathToStmtsMap,Map<String, String> invertedPathMap,
                                    String srcPath, String dstPath, ProgramElement srcEle, ProgramElement dstEle, int oneToMultiMapping) {
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
                    if (isContinue){
                        oneToMultiMapping++;
                        recordCrossFileMapping(srcPath, tmp_dstPath, srcEle, dstEle);
                    }
                }
            }
        }
        if (oneToMultiMapping > 1) {
            for (Map.Entry<Map<String, String>, String> entry : crossFileMap.entrySet()) {
                if (entry.getValue().equals(dstEle.getStringValue())) {
                    Map<String, String> pathToSrcStmt = entry.getKey();
                    for (String path : pathToSrcStmt.keySet()){
                        String srcStmt = pathToSrcStmt.get(path);
                        String[] parts = path.split("\\+");
                        String stmtMapping = srcStmt + " —> " + entry.getValue();
                        String srcFilePath = parts[0];
                        String srcPathAndMappingPair = srcFilePath + "+" + stmtMapping;
                        if (!oneToMultiMap.containsKey(srcPathAndMappingPair))
                            oneToMultiMap.put(srcPathAndMappingPair, new ArrayList<>());
                        oneToMultiMap.get(srcPathAndMappingPair).add(parts[1]);
                    }
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
    public Map<String, List<String>> getOneToMultiMap() {
        return oneToMultiMap;
    }
}
