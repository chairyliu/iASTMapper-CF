package cs.model.algorithm.element;

import com.github.gumtreediff.tree.ITree;
import cs.model.algorithm.ttmap.TokenRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Inner-stmt elements are those that compose a statement.
 */
public class InnerStmtElement extends AbstractElement implements ProgramElement {

    // token element representing the name of the inner-stmt element
    private ProgramElement nameToken = null;

    public InnerStmtElement(StmtElement stmtEle, ITree node){
        this.node = node;
        initInnerStmtElements();
        setStmtElement(stmtEle);
        setFromSrc(stmtEle.isFromSrc());
    }

    /**
     * Create an empty inner-stmt element.
     */
    public InnerStmtElement() {}

    /**
     * Check if the element is a null element
     */
    public boolean isNullElement() {
        return this.getITreeNode() == null;
    }

    @Override
    public void addTokenElement(TokenElement element) {
        if (tokenElements == null)
            tokenElements = new ArrayList<>();
        tokenElements.add(element);
    }

    @Override
    public List<TokenElement> getTokenElements() {
        if (tokenElements == null)
            getStmtElement().calTokensOfInnerStmtElements();
        if (tokenElements == null)
            tokenElements = new ArrayList<>();
        return tokenElements;
    }

    @Override
    public String getStringValue() {
        if (!"".equals(this.value))
            return this.value;

        if (isNullElement())
            return "";
        if ("".equals(this.value)) {
            for (TokenElement tokenEle: this.tokenElements) {
                this.value += tokenEle.getStringValue() + " ";
            }
            this.value = this.value.trim();
        }
        return this.value;
    }

    @Override
    public boolean equalValue(ProgramElement element) {
        if (this.getTokenElements().size() != element.getTokenElements().size())
            return false;
        for (int i = 0; i < this.getTokenElements().size(); i++) {
            TokenElement tokenEle1 = this.getTokenElements().get(i);
            TokenElement tokenEle2 = element.getTokenElements().get(i);
            if (!tokenEle1.equalValue(tokenEle2))
                return false;
        }
        return true;
    }

    @Override
    public boolean isStmt() {
        return false;
    }

    @Override
    public boolean isToken() {
        return false;
    }

    @Override
    public boolean isInnerStmtElement() {
        return true;
    }

    @Override
    public boolean isDeclaration() {
        if (typeChecker.isSingleVariableDeclaration(this.getITreeNode()))
            return true;
        if (typeChecker.isVariableDeclarationFragment(this.getITreeNode()))
            return true;
        return false;
    }

    @Override
    public ProgramElement getLeftSibling() {
        if (getChildIdx() == 0)
            return null;
        return getParentElement().getInnerStmtElements().get(getChildIdx() - 1);
    }

    @Override
    public ProgramElement getRightSibling() {
        int size = getParentElement().getInnerStmtElements().size();
        if (getChildIdx() == size - 1)
            return null;
        return getParentElement().getInnerStmtElements().get(getChildIdx() + 1);
    }

    @Override
    public String toString(){
        if (getTokenElements().size() > 0) {
            String tmpStr = "(line:" + getStartLine() + " " + getNodeType() + ") ";
            return tmpStr + getStringValue();
        } else {
            String tmpStr = "(line:" + getStartLine() + " " + getNodeType() + ") ";
            return tmpStr;
        }
    }

    /**
     * Get the token representing the name of the inner-stmt element
     */
    public ProgramElement getNameToken() {
        if (typeChecker.isQualifiedName(node)) {
            nameToken = getTokenElements().get(getTokenElements().size() - 1);
            return nameToken;
        }

        if (typeChecker.isMethodInvocationArguments(node)) {
            return null;
        }

        if (nameToken == null) {
            for (ITree t : node.getChildren()) {
                if (typeChecker.isSimpleName(t)) {
                    int startPos = t.getPos();
                    int endPos = t.getEndPos();
                    TokenRange range = new TokenRange(startPos, endPos);
                    StmtElement stmtEle = getStmtElement();
                    nameToken = stmtEle.getRangeElementMap().get(range);
                    break;
                }
            }
        }
        return nameToken;
    }
}
