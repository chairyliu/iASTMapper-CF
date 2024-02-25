package cs.model.algorithm.matcher.measures.stmt;

import com.github.gumtreediff.tree.ITree;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

/**
 * 比较两个方法声明语句的方法体是否具有相同的语法结构
 */
public class SameMethodBodyMeasure extends AbstractSimMeasure implements SimMeasure {
    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        StmtElement srcStmt = (StmtElement) srcEle;
        StmtElement dstStmt = (StmtElement) dstEle;
        //检查srcStmt是否为方法声明语句
        if (srcStmt.isMethodDec()) {
            //如果是，分别获取srcStmt和dstStmt中的方法体（methodBody），该方法会在给定的语法树节点中查找并返回方法体部分
            ITree srcMethodBody = getMethodBody(srcStmt.getITreeNode());
            ITree dstMethodBody = getMethodBody(dstStmt.getITreeNode());
//            System.out.println("Src Stmt MethodDec  " + srcStmt + "  Dst stmt MethodDec   "+ dstStmt + "  " + srcMethodBody.isIsomorphicTo(dstMethodBody));
//            System.out.println("Src Stmt MethodDec  " + srcMethodBody + "  Dst stmt MethodDec   "+ dstMethodBody);
            if (srcMethodBody == null || dstMethodBody == null)
                return 0;
            //比较两个方法体的语法结构是否同构，如果是，就返回相似性测量值为1，表示两个方法体结构相同
            if (srcMethodBody.isIsomorphicTo(dstMethodBody))
                return 1;
        }

        return 0;
    }

    private static ITree getMethodBody(ITree method) {
        for (ITree t: method.getChildren()) {
            if (typeChecker.isBlock(t))
                return t;
        }
        return null;
    }
}
