package cs.model.algorithm.matcher.measures;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMapping;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.innerstmt.InnerStmtEleSandwichMeasure;
import cs.model.algorithm.matcher.measures.innerstmt.InnerStmtEleTokenDiceMeasure;
import cs.model.algorithm.matcher.measures.innerstmt.InnerStmtSameStmtMeasure;
import cs.model.algorithm.matcher.measures.stmt.IdenticalStmtMeasure;
import cs.model.algorithm.matcher.measures.stmt.StmtDescendantMappingMeasure;
import cs.model.algorithm.matcher.measures.stmt.StmtExchangeMeasure;
import cs.model.algorithm.matcher.measures.stmt.textual.StmtTokenDiceMeasure;
import cs.model.algorithm.matcher.measures.token.TokenStmtMeasure;
import cs.model.algorithm.matcher.measures.token.Token_LRBMeasure;
import cs.model.algorithm.matcher.measures.util.ElementAncestorMeasure;
import cs.model.algorithm.matcher.measures.util.ElementTypeMeasure;

import java.util.HashMap;
import java.util.Map;

/**
 * APIs for calculating and using SimMeasures for
 * statements, tokens and inner-stmt elements.
 */
public class ElementSimMeasures {
    // source element
    private final ProgramElement srcEle;

    // target element
    private final ProgramElement dstEle;

    // This map stores the measure values.
    // The key is measure name and value is SimMeasure object
    private final Map<String, SimMeasure> measureMap;

    public ElementSimMeasures(ProgramElement srcE, ProgramElement dstE){
        this.srcEle = srcE;
        this.dstEle = dstE;
        this.measureMap = new HashMap<>();
        initMeasure();
    }

    private void initMeasure(){
        SimMeasure measure = new ElementTypeMeasure();
        measure.calSimMeasure(srcEle, dstEle);
        measureMap.put("E_TYPE", measure);
    }

    private int isSameStringValueForStmt() {
        if (srcEle.isStmt()) {
            String srcValue = srcEle.getStringValue();
            String dstValue = dstEle.getStringValue();
            if ("".equals(srcValue) || "".equals(dstValue))
                return 0;
            if (srcValue.equals(dstValue))
                return 1;
        }
        return 0;
    }

    public ElementMapping getElementMapping(){
        return new ElementMapping(srcEle, dstEle);
    }

    private void addNewMeasure(String measureName, ElementMappings eleMappings){
        SimMeasure measure = calSimMeasure(measureName, eleMappings);
        measureMap.put(measureName, measure);
    }

    public ProgramElement getSrcEle() {
        return srcEle;
    }

    public ProgramElement getDstEle() {
        return dstEle;
    }

    public Map<String, SimMeasure> getMeasureMap() {
        return measureMap;
    }

    public ProgramElement getAnotherElement(ProgramElement element) {
        if (element.isFromSrc())
            return getDstEle();
        else
            return getSrcEle();
    }

    /**
     * Get the similarity measure given a measure name
     * If measure is not calculated, this method first calculates the measure.
     *
     * @param measureName the wanted measure name
     * @param eleMappings currently calculated element mappings
     * @return the SimMeasure object
     */
    public SimMeasure getSimMeasure(String measureName, ElementMappings eleMappings) {
        if (measureMap.containsKey(measureName)) {
            return measureMap.get(measureName);
        } else {
            addNewMeasure(measureName, eleMappings);
            return measureMap.get(measureName);
        }
    }

    private void calMeasureValue(SimMeasure measure, ElementMappings eleMappings){
        measure.setElementMappings(eleMappings);
        measure.calSimMeasure(srcEle, dstEle);
    }

    private SimMeasure calSimMeasure(String measureName, ElementMappings eleMappings){
        SimMeasure measure = getSimMeasureObjByName(measureName);
        calMeasureValue(measure, eleMappings);
        return measure;
    }

    public static SimMeasure getSimMeasureObjByName(String measureName) {
        SimMeasure measure;
        switch(measureName){
            case SimMeasureNames.IDEN:
                measure = new IdenticalStmtMeasure();
                break;
            case SimMeasureNames.ANCE:
                measure = new ElementAncestorMeasure();
                break;
            case SimMeasureNames.IMSR:
                measure = new StmtDescendantMappingMeasure();
                break;
            case SimMeasureNames.IMTR:
                measure = new StmtTokenDiceMeasure();
                break;
            case SimMeasureNames.S_ABS:
                measure = new StmtExchangeMeasure();
                break;
            case SimMeasureNames.T_MSIS:
                measure = new TokenStmtMeasure();
                break;
            case SimMeasureNames.T_ABS:
                measure = new Token_LRBMeasure();
                break;
            case SimMeasureNames.I_MSIS:
                measure = new InnerStmtSameStmtMeasure();
                break;
            case SimMeasureNames.I_IMTR:
                measure = new InnerStmtEleTokenDiceMeasure();
                break;
            case SimMeasureNames.I_ABS:
                measure = new InnerStmtEleSandwichMeasure();
                break;
            default:
                throw new RuntimeException("do not support such measure now.");
        }
        return measure;
    }

    @Override
    public String toString() {
        String tmp = "" + srcEle + " => " + dstEle + " [";
        for (String measureName: measureMap.keySet()){
            tmp += measureName + ":" + measureMap.get(measureName).getValue();
            tmp += " ";
        }
        tmp += "]";
        return tmp;
    }

    /**
     * Compare between two measures given a measure name
     * @param measures1 first measures1
     * @param measures2 second measures
     * @param eleMappings current element mappings
     * @param measureName measure name
     * @return measures1 > measures2 ? 1 : 0
     */
    public static int doCompare(ElementSimMeasures measures1, ElementSimMeasures measures2,
                                 ElementMappings eleMappings, String measureName) {
        if (measures1.isSameStringValueForStmt() == 1 && measures2.isSameStringValueForStmt() == 1){
            if (measureName.equals(SimMeasureNames.IMTR))
                return 0;
        }

        SimMeasure measure1 = measures1.getSimMeasure(measureName, eleMappings);
        SimMeasure measure2 = measures2.getSimMeasure(measureName, eleMappings);
        return measure1.compare(measure2);
    }
}
