package cs.model.algorithm.matcher.measures;

import cs.model.algorithm.element.NGramCalculator;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.element.TokenElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.matchers.searchers.CandidateSetsAndMaps;
import cs.model.algorithm.matcher.matchers.searchers.FilterDstCandidates;

import java.util.Set;

/**
 * Base class of SimMeasure
 */
public abstract class AbstractSimMeasure implements SimMeasure {
    protected static NGramCalculator calculator = new NGramCalculator();

    protected ElementMappings elementMappings;
    protected double value;

    @Override
    public void setElementMappings(ElementMappings elementMappings) {
        this.elementMappings = elementMappings;
    }

    @Override
    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    protected boolean isParentMapping(ProgramElement srcEle, ProgramElement dstEle) {
        ProgramElement srcParentEle = srcEle.getParentElement();
        ProgramElement dstParentEle = dstEle.getParentElement();
        return elementMappings.getDstForSrc(srcParentEle) == dstParentEle;
    }

    @Override
    public void calSimMeasure(ProgramElement srcEle, ProgramElement dstEle) {
        this.value = calMeasureValue(srcEle, dstEle);
    }

    @Override
    public Set<ProgramElement> filterBadDstCandidateElements(ProgramElement srcEle, Set<ProgramElement> dstCandidates,
                                                             FilterDstCandidates filterDstCandidates,CandidateSetsAndMaps candidateSetsAndMaps) {
        return null;
    }

    protected abstract double calMeasureValue(ProgramElement srcEle, ProgramElement dstEle);

    public int compare(SimMeasure measure) {
        return compareMeasureVal(this.getValue(), measure.getValue());
    }

    protected int compareMeasureVal(double val1, double val2) {
        return Double.compare(val1, val2);
    }

    protected boolean isWithRename(ProgramElement srcElement, ProgramElement dstElement) {
        TokenElement srcNameToken = ((StmtElement) srcElement).getNameToken();
        TokenElement dstNameToken = ((StmtElement) dstElement).getNameToken();
        if (srcNameToken != null && dstNameToken != null) {
            return elementMappings.isTokenRenamed(srcNameToken, dstNameToken);
        }
        return false;
    }
}
