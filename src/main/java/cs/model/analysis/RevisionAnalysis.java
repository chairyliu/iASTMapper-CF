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
    protected String srcFilePath;

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
                            Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap){
        this.project = project;
        this.commitId = commitId;
        this.pathMap = pathMap;
        this.matcher = matcher;
        this.srcPathToMatcher = srcPathToMatcher;
        this.AllSrcPathToStmtsMap = AllSrcPathToStmtsMap;

        try{
            matcher.buildMappingsOuterLoop(srcStmts, srcToPath, pathMap, AllSrcPathToStmtsMap);//执行外层循环，建立元素映射及节点映射
            this.eleMappings = matcher.getEleMappings();
            srcRootEle = matcher.getSrcRootEle();
            calMappingRecords();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //遍历源代码中的语句，对于每一对源语句和目标语句，创建一个记录对象，并计算并设置相关的信息，然后将这些记录添加到mappingRecords列表中
    private void calMappingRecords(){
        mappingRecords = new ArrayList<>();
        List<ProgramElement> srcStmts = ElementTreeUtils.getAllStmtsPreOrder(srcRootEle);//获取源代码树中所有stmt列表
        for (ProgramElement srcStmt: srcStmts) {
            ProgramElement dstStmt = eleMappings.getDstForSrc(srcStmt);//获取与srcStmt对应映射的dstStmt
            if (dstStmt != null){
                StmtMappingAndMeasureRecord record = new StmtMappingAndMeasureRecord(project, commitId, srcFilePath);
                record.setStmtInfo((StmtElement) srcStmt, (StmtElement) dstStmt);//将srcStmt和dstStmt设置到record中
                record.setMeasures(srcStmt, dstStmt, eleMappings);
                mappingRecords.add(record);
            }
        }
    }

    public List<StmtTokenAction> generateActions() {
        if (actionList == null)
            actionList = matcher.generateStmtTokenEditActions();
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

//    public String getSrcFilePath() {
////        return srcFilePath;
//        return srcPath;
//    }

    public iASTMapper getMatcher() {
        return matcher;
    }
}
