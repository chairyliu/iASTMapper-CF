package cs.model.algorithm.matcher.fastmatchers;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.ttmap.TreeTokensMap;

import java.util.List;

public abstract class BaseFastMatcher {
    protected final List<ProgramElement> srcStmts;
    protected final List<ProgramElement> dstStmts;
    protected final ElementMappings elementMappings;
    protected TokenMapStatistics statistics;

    protected TreeTokensMap srcTtMap;
    protected TreeTokensMap dstTtMap;

    public BaseFastMatcher(List<ProgramElement> srcStmts, List<ProgramElement> dstStmts,
                           ElementMappings elementMappings) {
        this.srcStmts = srcStmts;
        this.dstStmts = dstStmts;
        this.elementMappings = elementMappings;
    }

    public void setTreeTokenMaps(TreeTokensMap srcTtMap, TreeTokensMap dstTtMap) {
        this.srcTtMap = srcTtMap;
        this.dstTtMap = dstTtMap;
    }

    public void setTokenMapStatistics(TokenMapStatistics statistics) {
        this.statistics = statistics;
    }

    public abstract void buildMappings();
}
