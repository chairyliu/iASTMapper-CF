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
//            if (value.equals("if(msg==null||msg.length()==0)msg=cause.toString();"))
//                System.out.println(value);
            if (!dstValueStmtMap.containsKey(value))
                continue;
//            Set<ProgramElement> dstStmts = dstValueStmtMap.get(value);
            List<ProgramElement> srcStmts = sortElementsByLineNumber(srcUnsortedStmts);
            List<ProgramElement> dstStmts = sortElementsByLineNumber(dstValueStmtMap.get(value));
            int srcSize = srcStmts.size();
            int dstSize = dstStmts.size();
//            if (srcStmts.size() > 1){
//                continue;
//            }
//            if (!dstValueStmtMap.containsKey(value))
//                continue;
//            Set<ProgramElement> dstStmts = dstValueStmtMap.get(value);
//            if (dstStmts.size() > 1)
//                continue;
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

//            ProgramElement srcStmtEle = srcStmts.iterator().next();//获取集合中的第一个元素
//            ProgramElement dstStmtEle = dstStmts.iterator().next();
////            System.out.println(srcStmtEle + "+" + dstStmtEle);
//            if (elementMappings.isMapped(srcStmtEle))
//                continue;
//            if (elementMappings.isMapped(dstStmtEle))
//                continue;
////            System.out.println("Src " + srcStmtEle + " || " + dstStmtEle);
//            addRecursiveMappings(srcStmtEle, dstStmtEle);
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

    //将源程序元素和目标程序元素的语句、内部语句以及token进行递归映射。
    private void addRecursiveMappings(ProgramElement srcEle, ProgramElement dstEle) {
        List<ProgramElement> srcStmts = ElementTreeUtils.getAllStmtsPreOrder(srcEle);
        List<ProgramElement> dstStmts = ElementTreeUtils.getAllStmtsPreOrder(dstEle);
        for (int i = 0; i < srcStmts.size(); i++) {
            ProgramElement srcStmtEle = srcStmts.get(i);
            ProgramElement dstStmtEle = dstStmts.get(i);
            elementMappings.addMapping(srcStmtEle, dstStmtEle);//源语句元素和目标语句元素的映射
//            System.out.println(i + " src " +srcStmtEle + " |And| " + dstStmtEle);
            for (int j = 0; j < srcStmtEle.getInnerStmtElements().size(); j++) {//对于每个语句，遍历其所有内部语句元素
                ProgramElement srcInnerStmtEle = srcStmtEle.getInnerStmtElements().get(j);
                ProgramElement dstInnerStmtEle = dstStmtEle.getInnerStmtElements().get(j);
//                System.out.println("src inner  " +srcInnerStmtEle + " |And| " + dstInnerStmtEle);
                elementMappings.addMapping(srcInnerStmtEle, dstInnerStmtEle);//将源和目标内部语句元素进行映射
            }
//            System.out.println("SrcStmt " + srcStmtEle + " ||| " + dstStmtEle + " " + srcStmtEle.getTokenElements().size() + " " + dstStmtEle.getTokenElements().size());
            // debug, repair the bug about the length error
            //对于每个语句，它遍历其token元素，使用Math.min确保在两个语句之间取较小的token数，以避免数组越界异常
            for (int j = 0; j < Math.min(srcStmtEle.getTokenElements().size(),dstStmtEle.getTokenElements().size()); j++) {
//            for (int j = 0; j < srcStmtEle.getTokenElements().size(); j++) {
                ProgramElement srcTokenEle = srcStmtEle.getTokenElements().get(j);
                ProgramElement dstTokenEle = dstStmtEle.getTokenElements().get(j);
//                System.out.println("src token  " +srcTokenEle + " |And| " + dstTokenEle);
                elementMappings.addMapping(srcTokenEle, dstTokenEle);//将源和目标令牌元素进行映射
            }
            tokenMapStatistics.recordAllTokenForStmt(srcStmtEle);
            tokenMapStatistics.recordAllTokenForStmt(dstStmtEle);//记录源和目标语句的所有token
        }
    }

    //遍历源程序中的语句集合，构建一个映射，将每个非代码块语句的内容与对应的程序元素关联起来
    private static void buildValueStmtMap(List<ProgramElement> srcStmts,
                                          TreeTokensMap ttMap,
                                          Map<String, Set<ProgramElement>> valueStmtMap){
        for (ProgramElement element: srcStmts){
            if (StaticNodeTypeChecker.getConfigNodeTypeChecker().isBlock(element.getITreeNode()))
                continue;
            //获取element对应的树节点的内容，同时移除其中的Javadoc注释
            String value = ttMap.getNodeContentAndRemoveJavadoc(element.getITreeNode());
//            System.out.println("First value  " + value);
            if (((StmtElement) element).isMethodDec())//如果element是一个方法声明，则将其祖先节点的值与其相连
                value = getAncestorValue(element) + value;
//            System.out.println("Second value  " + value);
            if (!value.equals("")) {
                if (!valueStmtMap.containsKey(value))//如果valueStmtMap中不包含键为value的映射
                    valueStmtMap.put(value, new HashSet<>());//则将其添加到valueStmtMap中，并关联一个新的空的HashSet
                valueStmtMap.get(value).add(element);//将当前element添加到与value关联的HashSet中
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
