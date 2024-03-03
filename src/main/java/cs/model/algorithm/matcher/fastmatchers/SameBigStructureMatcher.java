package cs.model.algorithm.matcher.fastmatchers;

import com.github.gumtreediff.tree.ITree;
import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.languageutils.typechecker.StaticNodeTypeChecker;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.utils.LongestCommonSubsequence;

import java.util.*;

/**
 * Fast token matcher.
 *
 * First, find the tokens with the same structure
 * Then, match those one-to-one mapped structures first
 * By doing so, in our iterative process, we sidestep a lot of tokens to be processed.
 */
public class SameBigStructureMatcher extends BaseFastMatcher {
    private final Set<ITree> srcMultiTokenNodes = new HashSet<>();
    private final Set<ITree> dstMultiTokenNodes = new HashSet<>();

    private final Map<String, Set<InnerStmtElement>> srcValEleMap = new HashMap<>();
    private final Map<String, Set<InnerStmtElement>> dstValEleMap = new HashMap<>();

    private final static int BIG_STRUCTURE_THRESHOLD = 5;

    public SameBigStructureMatcher(List<ProgramElement> srcStmts, List<ProgramElement> dstStmts,
                                   ElementMappings elementMappings) {
        super(srcStmts, dstStmts, elementMappings);
    }

    public void buildMappings(){
        prepareMultiTokenNodes();
        Set<String> multiTokenValues = new HashSet<>(srcValEleMap.keySet());
        Set<String> dstMultiTokenValues = dstValEleMap.keySet();
//        System.out.println("RetainAll : " + multiTokenValues);
        multiTokenValues.retainAll(dstMultiTokenValues);
//        System.out.println("set string : " + dstMultiTokenValues);
//        System.out.println("ALL Token values : " + multiTokenValues);

        for (String val: multiTokenValues){
            Set<InnerStmtElement> srcElements = srcValEleMap.get(val);
            Set<InnerStmtElement> dstElements = dstValEleMap.get(val);
            addElementToMap(srcElements, dstElements);
        }
    }

    //对两个InnerStmtElement的集合进行映射，调用一系列函数进行判断
    private void addElementToMap(Set<InnerStmtElement> srcElements,
                                 Set<InnerStmtElement> dstElements){
        boolean addMapping = srcElements.size() == 1 && dstElements.size() == 1;
        if (addMapping) {
            InnerStmtElement srcEle = srcElements.iterator().next();//从集合中获取唯一的InnerStmtElement
            InnerStmtElement dstEle = dstElements.iterator().next();
//            System.out.println("Src is " + srcEle + " Src Size is " + srcEle.getTokenElements().size());
//            System.out.println("dst is " + dstEle + " Dst Size is " + dstEle.getTokenElements().size());
            if (srcEle.getTokenElements().size() >= BIG_STRUCTURE_THRESHOLD) {//如果InnerStmt中token数量大于等于阈值
                boolean canBeMapped = determineCanMap(srcEle, dstEle);
                addMappingForMultiTokenElement(srcEle, dstEle, canBeMapped);
//                System.out.println("Src is " + srcEle + " || " + dstEle);
                addRecursiveMapping(srcEle, dstEle);
            }
        }
    }

    private List<InnerStmtElement> getPreorder(InnerStmtElement Ele){
        List<InnerStmtElement> res = new ArrayList<>();
        List<InnerStmtElement> tmp = new ArrayList<>();
        tmp.add(Ele);
        while (tmp.size() != 0){
            res.add(tmp.get(0));
            for (int i = 0; i < tmp.get(0).getInnerStmtElements().size(); i++) {
//                System.out.println("order " + tmp.get(0).getStringValue());
                tmp.add(tmp.get(0).getInnerStmtElements().get(i));
            }
            tmp.remove(0);
        }
        return res;
    }

    //将内部语句映射，并且处理了内部语句数量不同的情况
    private void addRecursiveMapping(InnerStmtElement srcEle, InnerStmtElement dstEle) {
//        for (int i = 0; i < srcEle.getInnerStmtElements().size(); i++) {
        // repair the bug that causal by the different size of the srcEle InnerStmt and the dstEle InnerStmt using LCS match

        //使用LCS的方法解决了因为内部语句元素数量不同而导致的问题
        if(srcEle.getInnerStmtElements().size() != dstEle.getInnerStmtElements().size()) {
            List<InnerStmtElement> srcSubInnerStmts = getPreorder(srcEle);
            List<InnerStmtElement> dstSubInnerStmts = getPreorder(dstEle);
            //寻找两个列表中的最长公共子序列
            LongestCommonSubsequence<InnerStmtElement> lcs = new LongestCommonSubsequence<InnerStmtElement>(srcSubInnerStmts, dstSubInnerStmts) {
                @Override
                public boolean isEqual(InnerStmtElement t1, InnerStmtElement t2) {
                    return t1.equalValue(t2);
                }
            };
            List<int[]> mappedIdxes = lcs.extractIdxes();//提取LCS的索引
            for (int[] idxes: mappedIdxes){//使用这些索引将srcSubInnerStmts和dstSubInnerStmts中的相应元素进行映射
                elementMappings.addMapping(srcSubInnerStmts.get(idxes[0]), dstSubInnerStmts.get(idxes[1]));
            }
        }
        else{//数量相同，则正常递归映射
            for (int i = 0; i < srcEle.getInnerStmtElements().size(); i++) {
    //            System.out.println(srcEle + " || " + dstEle + "  i is " + i + " " + srcEle.getInnerStmtElements().size() + " " + dstEle.getInnerStmtElements().size());
                InnerStmtElement srcChildEle = srcEle.getInnerStmtElements().get(i);
                InnerStmtElement dstChildEle = dstEle.getInnerStmtElements().get(i);
    //            System.out.println("Src child " + srcChildEle + " Dst child " + dstChildEle);
                elementMappings.addMapping(srcChildEle, dstChildEle);
                addRecursiveMapping(srcChildEle, dstChildEle);
            }
        }
    }

    //判断如果不是单变量声明后代的节点就可以继续映射
    private boolean determineCanMap(InnerStmtElement srcEle, InnerStmtElement dstEle) {
        ProgramElement srcStmtEle = srcEle.getStmtElement();//srcEle是innerStmt，srcStmtEle是stmt
        ProgramElement dstStmtEle = dstEle.getStmtElement();
//        System.out.println("Src and dst " + srcStmtEle + "   " + dstStmtEle);
        if (elementMappings.getDstForSrc(srcStmtEle) == dstStmtEle)
            return true;//如果elementMappings中已存在srcStmtEle到dstStmtEle的映射关系，直接返回true，表示可以映射
        // SingleVariableDeclaration in for loop or method declaration
        // may not be mapped across different statements.
        return !StaticNodeTypeChecker
                .getConfigNodeTypeChecker()
                .isDescendantOfSingleVariableDeclaration(srcEle.getITreeNode());//检查srcEle对应的树节点是否是单变量声明的后代
                //如果是，表示在for循环或方法声明中的 单变量声明 可能不能跨不同语句进行映射
    }

    //映射源和目标InnerStmtElement的token元素列表
    private void addMappingForMultiTokenElement(InnerStmtElement srcEle,
                                                InnerStmtElement dstEle,
                                                boolean canBeMapped){
        List<TokenElement> srcTokenElements = srcEle.getTokenElements();//得到token元素列表
        List<TokenElement> dstTokenElements = dstEle.getTokenElements();

        for (int i = 0; i < srcTokenElements.size(); i++){
            ProgramElement srcTokenEle = srcTokenElements.get(i);
            ProgramElement dstTokenEle = dstTokenElements.get(i);//遍历每个token元素
            if (canBeMapped) {
                if (!elementMappings.isMapped(srcTokenEle) && !elementMappings.isMapped(dstTokenEle)) {
                    elementMappings.addMapping(srcTokenEle, dstTokenEle);
                }
            }
        }
    }

    private void prepareMultiTokenNodes(){
        buildValInnerStmtEleMap(srcStmts);
        buildValInnerStmtEleMap(dstStmts);
    }

    private void buildValInnerStmtEleMap(List<ProgramElement> stmts) {
//        int num = 0;
        for (ProgramElement stmt: stmts){
            if (statistics.isAllTokenMapped(stmt))//检查token是否已被映射
                continue;
//            System.out.println(num + " Stmt is " + stmt);
            for (TokenElement token: stmt.getTokenElements()) {
//                System.out.println("Token is " + token);
                if (elementMappings.isMapped(token))
                    continue;
                //对于未映射的token，获取与该令牌元素关联的多令牌元素列表
                List<InnerStmtElement> multiTokenElements = token.getMultiTokenElementsWithToken();
                for (InnerStmtElement innerStmtElement: multiTokenElements)
                    addInnerStmtElementToValMap(innerStmtElement);
            }
//            num++;
        }
    }

    //将给定的InnerStmtElement对象关联的树节点添加到srcMultiTokenNodes中，并构建值与内部语句元素的映射
    private void addInnerStmtElementToValMap(InnerStmtElement element) {
        //这两个if是避免重复添加相同的InnerStmtElement到映射中
        if (element.isFromSrc() && srcMultiTokenNodes.contains(element.getITreeNode()))
            return;
        if (!element.isFromSrc() && dstMultiTokenNodes.contains(element.getITreeNode()))
            return;

        String value = element.getStringValue();//获取element的字符串值
        value = element.getNodeType() + ": " + value;// 并将其添加到节点类型的前缀后，构成用于映射的值
//        System.out.println("The value is " + value + " inner " + element);
        if (element.isFromSrc()) {
            srcMultiTokenNodes.add(element.getITreeNode());//将element关联的树节点添加到srcMultiTokenNodes
            if (!srcValEleMap.containsKey(value))//检查是否存在以value为键的映射
                srcValEleMap.put(value, new HashSet<>());
            srcValEleMap.get(value).add(element);//将element和value绑定
        }

        if (!element.isFromSrc()) {
            dstMultiTokenNodes.add(element.getITreeNode());
            if (!dstValEleMap.containsKey(value))
                dstValEleMap.put(value, new HashSet<>());
            dstValEleMap.get(value).add(element);
        }
    }
}
