package cs.model.algorithm.element;

import com.github.gumtreediff.tree.ITree;
import cs.model.algorithm.ttmap.TokenRange;
import cs.model.algorithm.ttmap.TokenRangeTypeMap;
import cs.model.algorithm.ttmap.TreeTokensMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * APIs for building element trees.
 */
public class ElementTreeBuilder {
    /**
     * Build an element tree based on an ITree.
     *
     * @param root root node of ITree
     * @param ttMap a tree token map
     * @param trtMap token and type map
     * @param isSrc if the analyzed file is source file
     * @return the root element of the element tree.
     */
    public static ProgramElement buildElementTree(ITree root, TreeTokensMap ttMap,
                                                  TokenRangeTypeMap trtMap, boolean isSrc){
        Map<ITree, ProgramElement> treeElementMap = new HashMap<>();//创建一个映射，将树节点和对应的程序元素关联起来
        //创建树的根元素，默认为RootElement，如果根节点是语句节点，则创建StmtElement
        AbstractElement rootEle = new RootElement();
        if (ProgramElement.typeChecker.isStatementNode(root))
            rootEle = new StmtElement();
        rootEle.initNearestDescendantStmts();
        rootEle.initTokenElements();
        rootEle.setRoot(true);//将根元素标记为树的根
        rootEle.setFromSrc(isSrc);//设置根元素rootEle是否来自源代码，根据布尔值isSrc
        rootEle.setITreeNode(root);
        treeElementMap.put(root, rootEle);//将树节点root和根元素rootEle放入映射
        for (ITree t: root.preOrder()){
            if (t == root)
                continue;
            if (ProgramElement.typeChecker.isStatementNode(t)){//如果节点是语句节点，则创建相应的StmtElement
                ProgramElement stmtEle = createStmtElement(t, ttMap, isSrc,
                        treeElementMap, trtMap, rootEle);
                treeElementMap.put(t, stmtEle);//将语句元素和树节点对应起来
            } else {
                if (isJavadocRelated(t))
                    continue;
                ITree stmt = findNearestAncestorStmt(t);//查找最近的祖先语句节点
                if (stmt != null) {
                    ProgramElement stmtEle = treeElementMap.get(stmt);//stmt是树节点，从映射中找到对应的语句元素
                    InnerStmtElement element = ((StmtElement) stmtEle).addInnerStmtElement(t);//并创建内部语句元素

                    ITree parent = t.getParent();//内部语句的父节点
                    ProgramElement parentEle = treeElementMap.get(parent);//内部语句的父元素
                    element.setParentElement(parentEle);
                    element.setChildIdx(parentEle.getInnerStmtElements().size());//设置内部语句元素在父元素中的索引
                    parentEle.addInnerStmtElement(element);//将内部语句元素添加到父元素的内部语句元素列表中
                    element.setStartLine(ttMap.getStartLineOfNode(t.getPos()));//设置内部语句元素的起始行
                    treeElementMap.put(t, element);
                }
            }
        }
        //获取所有语句元素的列表
        List<ProgramElement> allStmts = ElementTreeUtils.getAllStmtsPreOrder(rootEle);
        for (ProgramElement stmt: allStmts) {
            for (TokenElement token: stmt.getTokenElements()) {//得到所有的token元素
                ProgramElement element = treeElementMap.get(token.getITreeNode());//获取token的树节点对应的token元素（element）
                if (element.isInnerStmtElement())//如果是内部语句元素，则设置为token的内部语句
                    token.setInnerStmtEleOfToken((InnerStmtElement) element);
            }
        }

        return rootEle;
    }

    private static ITree findNearestAncestorStmt(ITree t) {
        ITree tmp = t;
        while (tmp != null && !ProgramElement.typeChecker.isStatementNode(tmp))
            tmp = tmp.getParent();
        return tmp;
    }

    private static boolean isJavadocRelated(ITree t) {
        ITree tmp = t;
        while (tmp != null) {
            if (ProgramElement.typeChecker.isJavaDoc(tmp))
                return true;
            tmp = tmp.getParent();
        }
        return false;
    }

    private static ProgramElement createStmtElement(ITree stmt, TreeTokensMap ttMap, boolean isSrc,
                                                    Map<ITree, ProgramElement> treeElementMap,
                                                    TokenRangeTypeMap tokenTypeMap,
                                                    ProgramElement rootElement) {
        List<TokenRange> tokens = ttMap.getTokenRangesOfNode(stmt);
        StmtElement stmtEle = new StmtElement();
        stmtEle.initNearestDescendantStmts();
        stmtEle.initTokenElements();
        stmtEle.setFromSrc(isSrc);
        stmtEle.setITreeNode(stmt);
        stmtEle.setStartLine(ttMap.getStartLineOfNode(stmt.getPos()));
        ProgramElement directParentElement = getDirectParentElement(stmtEle,  treeElementMap, rootElement);
        stmtEle.setParentElement(directParentElement);
        stmtEle.setChildIdx(directParentElement.getNearestDescendantStmts().size());
        directParentElement.addNearestDescendantStmts(stmtEle);
        String stmtValue = "";

        ElementNameCalculator calculator = new ElementNameCalculator(stmtEle, ttMap);
        String name = calculator.getName();
        stmtEle.setName(name);

        for (TokenRange token: tokens){
            ITree tokenNode = ttMap.getTokenRangeTreeMap().get(token);
            TokenElement tokenEle = new TokenElement();
            tokenEle.setStartLine(ttMap.getStartLineOfNode(token.first));
            tokenEle.setFromSrc(isSrc);
            tokenEle.setITreeNode(tokenNode);
            tokenEle.setTokenRange(token);
            String tokenValue = ttMap.getTokenByRange(token);
            tokenEle.setStringValue(tokenValue);
            String tokenType = tokenTypeMap.getTokenType(token);
            tokenEle.setTokenType(tokenType);
            tokenEle.setChildIdx(stmtEle.getTokenElements().size());
            tokenEle.setStmtElement(stmtEle);
            tokenEle.setParentElement(stmtEle);
            stmtEle.addTokenElement(tokenEle);
            stmtValue += tokenValue + " ";
        }

        if (stmtEle.getTokenElements().size() > 0)
            stmtEle.setStartLine(stmtEle.getTokenElements().get(0).getStartLine());

        stmtEle.setStringValue(stmtValue);
        return stmtEle;
    }

    private static ProgramElement getDirectParentElement(ProgramElement ele,
                                                         Map<ITree, ProgramElement> treeElementMap,
                                                         ProgramElement rootElement){
        ITree treeNode = ele.getITreeNode();
        ITree tmpNode = treeNode.getParent();
        while (tmpNode != null){
            if (ProgramElement.typeChecker.isStatementNode(tmpNode))
                return treeElementMap.get(tmpNode);
            tmpNode = tmpNode.getParent();
        }
        return rootElement;
    }
}
