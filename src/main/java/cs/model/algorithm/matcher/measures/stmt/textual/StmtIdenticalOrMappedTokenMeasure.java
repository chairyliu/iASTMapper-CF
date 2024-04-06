package cs.model.algorithm.matcher.measures.stmt.textual;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.AbstractSimMeasure;
import cs.model.algorithm.matcher.measures.SimMeasure;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mechanism: mapped statements should have similar content.
 */
public class StmtIdenticalOrMappedTokenMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        if (srcEle.equalValue(dstEle)){
            if (!onlyNameAndLiteral)
                return srcEle.getTokenElements().size();
            else
                return ((StmtElement) srcEle).getNameAndLiteralNum();
        }
        return calExtremeIdenticalToken((StmtElement) srcEle, (StmtElement) dstEle) + calExtremeMappingToken((StmtElement) srcEle, (StmtElement) dstEle);
    }

    private double calExtremeIdenticalToken(StmtElement srcEle, StmtElement dstEle){
        Map<String, Integer> srcTokenNumMap = new HashMap<>(srcEle.getTokenNumMap(onlyNameAndLiteral));
        Map<String, Integer> dstTokenNumMap = new HashMap<>(dstEle.getTokenNumMap(onlyNameAndLiteral));

        double val = 0;
        for (String tokenValue: srcTokenNumMap.keySet()) {
            int num1 = srcTokenNumMap.get(tokenValue);
            if (!dstTokenNumMap.containsKey(tokenValue))
                continue;
            int num2 = dstTokenNumMap.get(tokenValue);
            int minVal = Integer.min(num1, num2);
            val += minVal;
            srcTokenNumMap.put(tokenValue, num1 - minVal);
            dstTokenNumMap.put(tokenValue, num2 - minVal);
        }
        return val;
    }

    private double calExtremeMappingToken(StmtElement srcEle, StmtElement dstEle) {
        Map<String, Set<TokenElement>> srcMappingTokenNumMap = new HashMap<>(srcEle.getValueTokenElementMap(onlyNameAndLiteral));
        Map<String, Set<TokenElement>> dstMappingTokenNumMap = new HashMap<>(dstEle.getValueTokenElementMap(onlyNameAndLiteral));

        double val = 0;

        for (String mappingValue: srcMappingTokenNumMap.keySet()){
            Set<TokenElement> srcMappingEle = srcMappingTokenNumMap.get(mappingValue);
            if (!dstMappingTokenNumMap.containsKey(mappingValue))
                continue;
            Set<TokenElement> dstMappingEle = dstMappingTokenNumMap.get(mappingValue);
            val += getMappedToeknsValue(srcMappingEle, dstMappingEle, elementMappings);
        }
        return val;
    }

    public static double getMappedToeknsValue(Set<TokenElement> srcTokens, Set<TokenElement> dstTokens, ElementMappings elementMappings){
        int tmpNum = 0;
        for (TokenElement srcToken : srcTokens){
            if (elementMappings.isMapped(srcToken)){
                if (dstTokens.contains(elementMappings.getMappedElement(srcToken)))
                    tmpNum += 1.0;
            }
        }
        return tmpNum;
    }
}
