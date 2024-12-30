package cs.model.algorithm.element;

import java.util.ArrayList;
import java.util.List;

public class ElementTreeUtils {
    /**
     * return a list of every stmts and the stmts ordered using a pre-order
     * @param rootElement a root element
     */
    public static List<ProgramElement> getAllStmtsPreOrder(ProgramElement rootElement) {
        List<ProgramElement> elements = new ArrayList<>();
        preOrder(rootElement, elements);
        return elements;
    }

    private static void preOrder(ProgramElement element, List<ProgramElement> elements) {
        if (element.isStmt())
            elements.add(element);
        List<StmtElement> stmtElements = element.getNearestDescendantStmts();
        for (ProgramElement stmtEle: stmtElements)
            preOrder(stmtEle, elements);
    }
}
