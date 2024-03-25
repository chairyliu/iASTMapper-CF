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

public class StmtMappingTokenMeasure extends AbstractSimMeasure implements SimMeasure {

    @Override
    protected double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle) {
        if (srcEle.equalValue(dstEle)){
            if (!onlyNameAndLiteral)
                return srcEle.getTokenElements().size();
            else
                return ((StmtElement) srcEle).getNameAndLiteralNum();
        }
        return calExtremeMappingToken((StmtElement) srcEle, (StmtElement) dstEle);
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
            val += getSetSameValue(srcMappingEle, dstMappingEle, elementMappings);
        }
        return val;
    }

    public static double getSetSameValue(Set<TokenElement> srcTokens, Set<TokenElement> dstTokens, ElementMappings elementMappings){
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
