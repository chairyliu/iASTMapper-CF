package cs.model.algorithm.matcher.rules.innerstmt;

import cs.model.algorithm.element.InnerStmtElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.rules.AbstractElementMatchRule;
import cs.model.algorithm.matcher.rules.ElementMatchRule;

public class AnonymousDecRule extends AbstractElementMatchRule implements ElementMatchRule {
    public  AnonymousDecRule(){
        super();
    }
    @Override
    public boolean determineCanBeMapped(ElementSimMeasures measures, ElementMappings eleMappings) {
        //检查measures中的源元素是否是匿名类声明
        if (typeChecker.isAnonymousClassDec(measures.getSrcEle().getITreeNode())) {
//            System.out.println("AnonymousClassDec is " + measures.getSrcEle());
            InnerStmtElement srcEle = (InnerStmtElement) measures.getSrcEle();
            InnerStmtElement dstEle = (InnerStmtElement) measures.getDstEle();
            //通过 eleMappings 对象检查 srcEle 的父元素是否已经映射到 dstEle 的父元素
            return eleMappings.getMappedElement(srcEle.getParentElement()) == dstEle.getParentElement();
        }
        //表示源元素和目标元素不能进行映射，返回false
        return false;
    }
}
