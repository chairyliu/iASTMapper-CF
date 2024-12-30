package cs.model.algorithm.matcher.fastmatchers;

import cs.model.algorithm.element.ElementTreeUtils;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.languageutils.typechecker.StaticNodeTypeChecker;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.ttmap.TreeTokensMap;

import java.util.*;

/**
 * Fast Stmt Matcher.
 *
 * Maps identical statements.
 * Only identical declarations with identical ancestors can be mapped.
 */
public class SameStmtMatcher extends BaseFastMatcher {
    private final Map<String, Set<ProgramElement>> srcValueStmtMap;
    private final Map<String, Set<ProgramElement>> dstValueStmtMap;
    private final TokenMapStatistics tokenMapStatistics;

    public SameStmtMatcher(List<ProgramElement> srcStmtElements,
                           List<ProgramElement> dstStmtElements,
                           ElementMappings elementMappings,
                           TokenMapStatistics tokenMapStatistics) {
        super(srcStmtElements, dstStmtElements, elementMappings);
        this.srcValueStmtMap = new HashMap<>();
        this.dstValueStmtMap = new HashMap<>();
        this.tokenMapStatistics = tokenMapStatistics;
    }

    @Override
    public void buildMappings(){
        buildValueStmtMap(srcStmts, srcTtMap, srcValueStmtMap);
        buildValueStmtMap(dstStmts, dstTtMap, dstValueStmtMap);

        for (String value: srcValueStmtMap.keySet()){
            Set<ProgramElement> srcUnsortedStmts = srcValueStmtMap.get(value);
            if (!dstValueStmtMap.containsKey(value))
                continue;
            List<ProgramElement> srcStmts = sortElementsByLineNumber(srcUnsortedStmts);
            List<ProgramElement> dstStmts = sortElementsByLineNumber(dstValueStmtMap.get(value));
            int srcSize = srcStmts.size();
            int dstSize = dstStmts.size();
            if (srcSize == dstSize) {
                Iterator<ProgramElement> srcIterator = srcStmts.iterator();
                Iterator<ProgramElement> dstIterator = dstStmts.iterator();
                while (srcIterator.hasNext() && dstIterator.hasNext()) {
                    ProgramElement srcStmtEle = srcIterator.next();
                    ProgramElement dstStmtEle = dstIterator.next();
                    if (elementMappings.isMapped(srcStmtEle) || elementMappings.isMapped(dstStmtEle))
                        continue;
                    addRecursiveMappings(srcStmtEle, dstStmtEle);
                }
            }
        }
    }

    private List<ProgramElement> sortElementsByLineNumber(Set<ProgramElement> elements) {
        List<ProgramElement> sortedList = new ArrayList<>(elements);
        Collections.sort(sortedList, new Comparator<ProgramElement>() {
            @Override
            public int compare(ProgramElement p1, ProgramElement p2) {
                return Integer.compare(p1.getStartLine(), p2.getStartLine());
            }
        });
        return sortedList;
    }

    private void addRecursiveMappings(ProgramElement srcEle, ProgramElement dstEle) {
        List<ProgramElement> srcStmts = ElementTreeUtils.getAllStmtsPreOrder(srcEle);
        List<ProgramElement> dstStmts = ElementTreeUtils.getAllStmtsPreOrder(dstEle);
        int stmtMinSize = Math.min(srcStmts.size(),dstStmts.size());
        for (int i = 0; i < stmtMinSize; i++) {
            ProgramElement srcStmtEle = srcStmts.get(i);
            ProgramElement dstStmtEle = dstStmts.get(i);
            elementMappings.addMapping(srcStmtEle, dstStmtEle);
            int srcSize = srcStmtEle.getInnerStmtElements().size();
            int dstSize = dstStmtEle.getInnerStmtElements().size();
            int minSize = Math.min(srcSize, dstSize);
            if (minSize > 0) {
                for (int j = 0; j < minSize; j++) {
                    ProgramElement srcInnerStmtEle = srcStmtEle.getInnerStmtElements().get(j);
                    ProgramElement dstInnerStmtEle = dstStmtEle.getInnerStmtElements().get(j);
                    elementMappings.addMapping(srcInnerStmtEle, dstInnerStmtEle);
                }
            }

            // debug, repair the bug about the length error
            for (int j = 0; j < Math.min(srcStmtEle.getTokenElements().size(),dstStmtEle.getTokenElements().size()); j++) {
                ProgramElement srcTokenEle = srcStmtEle.getTokenElements().get(j);
                ProgramElement dstTokenEle = dstStmtEle.getTokenElements().get(j);
                elementMappings.addMapping(srcTokenEle, dstTokenEle);
            }
            tokenMapStatistics.recordAllTokenForStmt(srcStmtEle);
            tokenMapStatistics.recordAllTokenForStmt(dstStmtEle);
        }
    }

    private static void buildValueStmtMap(List<ProgramElement> srcStmts,
                                          TreeTokensMap ttMap,
                                          Map<String, Set<ProgramElement>> valueStmtMap){
        for (ProgramElement element: srcStmts){
            if (StaticNodeTypeChecker.getConfigNodeTypeChecker().isBlock(element.getITreeNode()))
                continue;
            String value = ttMap.getNodeContentAndRemoveJavadoc(element.getITreeNode());
            if (((StmtElement) element).isMethodDec())
                value = getAncestorValue(element) + value;
            if (!value.equals("")) {
                if (!valueStmtMap.containsKey(value))
                    valueStmtMap.put(value, new HashSet<>());
                valueStmtMap.get(value).add(element);
            }
        }
    }

    private static String getAncestorValue(ProgramElement element) {
        ProgramElement parent = element.getParentElement();
        String ret = "";
        while (!parent.isRoot()) {
            if (parent.isDeclaration()) {
                ret += "[" + parent.getNodeType() + ":" + parent.getName() + "]";
            }
            parent = parent.getParentElement();
        }
        return ret;
    }
}
