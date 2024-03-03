package cs.model.algorithm.matcher.fastmatchers;

import cs.model.algorithm.element.MethodParameterType;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;

import java.util.*;

/**
 * Fast Method Matcher.
 *
 * Directly maps two methods with identical signature.
 * For method signature, we only consider method name, types of the
 */
public class MethodMatcherWithSameSignature extends BaseFastMatcher {
    private final Map<String, List<ProgramElement>> srcNameMethodMap;//键是方法名称
    private final Map<String, List<ProgramElement>> dstNameMethodMap;

    /**
     * Constructor
     * @param srcStmts list of source statement elements
     * @param dstStmts list of target statement elements
     * @param elementMappings current element mappings
     */
    public MethodMatcherWithSameSignature(List<ProgramElement> srcStmts, List<ProgramElement> dstStmts,
                                          ElementMappings elementMappings) {
        super(srcStmts, dstStmts, elementMappings);
        this.srcNameMethodMap = new HashMap<>();
        this.dstNameMethodMap = new HashMap<>();
    }

    public void buildMappings(){//找出名字相同的两个方法声明
        buildNameMethodMap(srcStmts, srcNameMethodMap);//键是语句元素，值是对应的方法名称map集合
        buildNameMethodMap(dstStmts, dstNameMethodMap);

        for (String name: srcNameMethodMap.keySet()) {
            List<ProgramElement> srcMethods = srcNameMethodMap.get(name);
            List<ProgramElement> dstMethods = dstNameMethodMap.get(name);
            if (dstMethods == null || dstMethods.size() != 1)
                continue;
//            System.out.println("Src Method is " + srcMethods + "   Dst Method is " + dstMethods);
            buildMapping(srcMethods, dstMethods);
        }
    }

    // only consider to map methods with identical signature (name + type list)
    private void buildMapping(List<ProgramElement> srcMethods, List<ProgramElement> dstMethods) {//参数列表匹配的两个方法
        Map<ProgramElement, Set<ProgramElement>> srcToMultiDst = new HashMap<>();//源方法到多个目标方法的映射关系
        Map<ProgramElement, Set<ProgramElement>> dstToMultiSrc = new HashMap<>();//目标方法到多个源方法的映射关系
        for (ProgramElement srcMethod: srcMethods) {
            for (ProgramElement dstMethod: dstMethods) {
                List<MethodParameterType> typeList1 = ((StmtElement) srcMethod).getMethodTypeList();//逐个获取每条语句中每个token的类型，构成List
                List<MethodParameterType> typeList2 = ((StmtElement) dstMethod).getMethodTypeList();
//                System.out.println("SrecMehod " + srcMethod + " " + dstMethod);
//                System.out.println("Typelist  " + typeList1 + " " + typeList2);
                if (MethodParameterType.isIdenticalMethodParameterTypeList(typeList1, typeList2, elementMappings)){//遍历比较list中的每个类型
                    if (!srcToMultiDst.containsKey(srcMethod))
                        srcToMultiDst.put(srcMethod, new HashSet<>());//键是源方法，值用于后续添加srcmethod对应的所有候选dstmethod
                    if (!dstToMultiSrc.containsKey(dstMethod))
                        dstToMultiSrc.put(dstMethod, new HashSet<>());
                    srcToMultiDst.get(srcMethod).add(dstMethod);
                    dstToMultiSrc.get(dstMethod).add(srcMethod);
//                    System.out.println("True");
                }
            }
        }
        //如果srcmethod只有一个对应的dstmethod，该dstmethod也只对应该srcmethod，加入到elementMappings中
        for (ProgramElement srcEle: srcToMultiDst.keySet()) {
            Set<ProgramElement> dstElements = srcToMultiDst.get(srcEle);
            if (dstElements.size() == 1) {
                ProgramElement dstEle = dstElements.iterator().next();
                if (dstToMultiSrc.get(dstEle).size() == 1){
                    elementMappings.addMapping(srcEle, dstEle);
                }
            }
        }
    }

    private void buildNameMethodMap(List<ProgramElement> stmts, Map<String, List<ProgramElement>> nameMethodMap) {
        for (ProgramElement srcEle: stmts) {
            if (elementMappings.isMapped(srcEle))
                continue;
            if (((StmtElement) srcEle).isMethodDec()) {
                String name = srcEle.getName();
//                System.out.println("src " + srcEle + " name " + name);
                if (!nameMethodMap.containsKey(name))
                    nameMethodMap.put(name, new ArrayList<>());
                nameMethodMap.get(name).add(srcEle);
            }
        }
    }
}
