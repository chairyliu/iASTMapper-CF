package cs.model.analysis;

import cs.model.algorithm.actions.StmtTokenAction;
import cs.model.algorithm.actions.TreeEditAction;
import cs.model.algorithm.element.ElementTreeUtils;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.iASTMapper;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.evaluation.csvrecord.measure.StmtMappingAndMeasureRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Perform analysis with our method on a file revision
 */
public class RevisionAnalysis {
    protected String project;
    protected String commitId;
    protected String srcToPath;
    protected Map<String, String> pathMap;
    private iASTMapper matcher;
    private List<StmtMappingAndMeasureRecord> mappingRecords;
    private ProgramElement srcRootEle;
    private List<StmtTokenAction> actionList = null;
    private ElementMappings eleMappings;
    public Map<String, iASTMapper> srcPathToMatcher;
    public static Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap;

    public RevisionAnalysis(String project, String commitId, String srcToPath,Map<String, String> pathMap, iASTMapper matcher,
                            Map<String, iASTMapper> srcPathToMatcher, List<ProgramElement> srcStmts,
                            Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap, boolean isSingleFile, boolean doCrossFileMapping){
        this.project = project;
        this.commitId = commitId;
        this.srcToPath = srcToPath;
        this.pathMap = pathMap;
        this.matcher = matcher;
        this.srcPathToMatcher = srcPathToMatcher;
        this.AllSrcPathToStmtsMap = AllSrcPathToStmtsMap;

        try{
            matcher.buildMappingsOuterLoop(srcStmts, srcToPath, pathMap, AllSrcPathToStmtsMap, isSingleFile, doCrossFileMapping);
            this.eleMappings = matcher.getEleMappings();
            srcRootEle = matcher.getSrcRootEle();
            storeMappingRecords();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Calculate and store mapping records between source and target statements,
     * mapping records means the mapping information and measures.
     */
    private void storeMappingRecords(){
        mappingRecords = new ArrayList<>();
        List<ProgramElement> srcStmts = ElementTreeUtils.getAllStmtsPreOrder(srcRootEle);
        for (ProgramElement srcStmt: srcStmts) {
            ProgramElement dstStmt = eleMappings.getDstForSrc(srcStmt);
            if (dstStmt != null){
                StmtMappingAndMeasureRecord record = new StmtMappingAndMeasureRecord(project, commitId, srcToPath);
                record.setStmtInfo((StmtElement) srcStmt, (StmtElement) dstStmt);
                record.setMeasures(srcStmt, dstStmt, eleMappings);
                mappingRecords.add(record);
            }
        }
    }

    public List<StmtTokenAction> generateActions(String srcPath) {
        if (actionList == null)
            actionList = matcher.generateStmtTokenEditActions(srcPath);
        return actionList;
    }

    public List<TreeEditAction> generateEditActions(){
        return matcher.getTreeEditActions();
    }

    public List<String[]> getMappingRecords() {
        List<String[]> ret = new ArrayList<>();
        if (mappingRecords.size() > 0) {
            for (StmtMappingAndMeasureRecord record: mappingRecords) {
                ret.add(record.toRecord());
            }
        }
        return ret;
    }

    public iASTMapper getMatcher() {
        return matcher;
    }
}
