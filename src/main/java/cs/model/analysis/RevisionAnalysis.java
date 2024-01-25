package cs.model.analysis;

import cs.model.algorithm.actions.StmtTokenAction;
import cs.model.algorithm.actions.TreeEditAction;
import cs.model.algorithm.element.ElementTreeUtils;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.iASTMapper;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.evaluation.csvrecord.measure.StmtMappingAndMeasureRecord;
import cs.model.gitops.GitUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Perform analysis with our method on a file revision
 */
public class RevisionAnalysis {
    protected String project;
    protected String commitId;
    protected String srcFilePath;
    protected String dstFilePath;
    protected String srcFileContent;
    protected String dstFileContent;

    private iASTMapper matcher;
    private List<StmtMappingAndMeasureRecord> mappingRecords;
    private ProgramElement srcRootEle;
    private ProgramElement dstRootEle;
    private List<StmtTokenAction> actionList = null;
    private ElementMappings eleMappings;

    public RevisionAnalysis(String project, String commitId, String baseCommitId,
                            String srcFilePath, String dstFilePath) throws Exception{
        this.project = project;//需要后期处理冗余
        this.commitId = commitId;
        this.srcFilePath = srcFilePath;
        this.dstFilePath = dstFilePath;

        System.out.println(srcFilePath);
        try {
            ByteArrayOutputStream srcFileStream = GitUtils
                    .getFileContentOfCommitFile(project, baseCommitId, srcFilePath);//获取源文件和修订后文件的内容
            srcFileContent = srcFileStream.toString("UTF-8");
            if (srcFileContent.equals("")){
                this.srcFilePath = null;
                return;
            }
            ByteArrayOutputStream dstFileStream = GitUtils
                    .getFileContentOfCommitFile(project, commitId, dstFilePath);
            dstFileContent = dstFileStream.toString("UTF-8");
            if (dstFileContent.equals("")) {
                this.dstFilePath = null;
                return;
            }
            matcher = new iASTMapper(srcFileContent, dstFileContent);//创建iASTMapper对象
            matcher.buildMappingsOuterLoop();//执行外层循环，建立元素映射及节点映射
            this.eleMappings = matcher.getEleMappings();
            srcRootEle = matcher.getSrcRootEle();
            dstRootEle = matcher.getDstRootEle();
            calMappingRecords();
        } catch (Exception e){
            e.printStackTrace();
            this.srcFilePath = null;
            this.dstFilePath = null;
            throw new RuntimeException(e.getMessage());
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

    public String getSrcFilePath() {
        return srcFilePath;
    }

    public String getDstFilePath() {
        return dstFilePath;
    }

    public String getSrcFileContent() {
        return srcFileContent;
    }

    public String getDstFileContent() {
        return dstFileContent;
    }

    public iASTMapper getMatcher() {
        return matcher;
    }
}
