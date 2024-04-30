package cs.model.analysis;

import cs.model.algorithm.actions.StmtTokenAction;
import cs.model.algorithm.actions.TreeEditAction;
import cs.model.algorithm.element.ElementTreeUtils;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.element.StmtElement;
import cs.model.algorithm.iASTMapper;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.evaluation.csvrecord.measure.StmtMappingAndMeasureRecord;
import cs.model.gitops.GitHunk;
import cs.model.gitops.GitUtils;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Perform analysis with our method on a file revision
 */
public class RevisionAnalysis {
    protected String project;
    protected String commitId;
    protected String srcFilePath;
    protected String dstFilePath;
    protected String srcPath;
    protected String srcFileContent;
    protected String dstFileContent;

    protected Map<String, String> pathMap;

    private iASTMapper matcher;
    private List<StmtMappingAndMeasureRecord> mappingRecords;
    private ProgramElement srcRootEle;
//    private ProgramElement dstRootEle;
    private List<StmtTokenAction> actionList = null;
    private ElementMappings eleMappings;
    public static Map<String, List<ProgramElement>> srcStmtsToMap;
    public static Map<String, ProgramElement> dstPathToRoot;
    public static List<ProgramElement> AllDstStmtsToMap;
    public static List<ProgramElement> AllDstTokensToMap;
    public static List<ProgramElement> AllDstinnerStmtsToMap;
    public static Map<String, Set<ProgramElement>> AllDstPathToStmtsMap;
    public static Map<String, Set<ProgramElement>> AllDstPathToTokensMap;
    public static Map<String, Set<ProgramElement>> AllDstPathToinnerStmtsMap;
    public Map<String, Set<ProgramElement>> AllDstValTokenMap;
    public Map<String, iASTMapper> srcPathToMatcher = new HashMap<>();

    public RevisionAnalysis(String project, String commitId,
                            String baseCommitId, Map<String, String> pathMap) throws Exception{
        this.project = project;
        this.commitId = commitId;
        this.pathMap = pathMap;
//        this.srcFilePath = srcFilePath;//删
//        this.dstFilePath = dstFilePath;//删
        srcStmtsToMap = new HashMap<>();
        dstPathToRoot = new HashMap<>();
        this.AllDstPathToStmtsMap = new HashMap<>();
        this.AllDstPathToTokensMap = new HashMap<>();
        this.AllDstPathToinnerStmtsMap = new HashMap<>();
        this.AllDstStmtsToMap = new ArrayList<>();
        this.AllDstTokensToMap = new ArrayList<>();
        this.AllDstinnerStmtsToMap = new ArrayList<>();
        this.AllDstValTokenMap = new HashMap<>();

        //这里根据传入的pathMap进行for遍历,可以先把<srcFileContent, dstFileContent>都存下来，然后进iASTMapper之前用for遍历每个srcFileContent对应的所有dstFileContent
        //iASTMapper中的步骤如果有重复也可以提前计算，避免太高的时间复杂度
        boolean isLastPath = false;
        int i = 0;
        for (String srcFilePath : pathMap.keySet()){
            i++;
            if (i == pathMap.size()) isLastPath = true;
            String dstFilePath = pathMap.get(srcFilePath);
            if (dstFilePath == null)
                continue;
            if (checkOnlyRenameOperation(project, baseCommitId, commitId, srcFilePath, dstFilePath))
                continue;
            if (checkAddedOrDeletedLines(srcFilePath, dstFilePath))
                continue;
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
                matcher = new iASTMapper(srcFileContent, dstFileContent, srcFilePath, dstFilePath, srcStmtsToMap,dstPathToRoot);//创建iASTMapper对象
                matcher.multiFastMapped();
                matcher.preStoreAllDstCandidates(srcFilePath, dstFilePath, isLastPath,AllDstStmtsToMap, AllDstTokensToMap,
                        AllDstinnerStmtsToMap,AllDstPathToStmtsMap, AllDstPathToTokensMap, AllDstPathToinnerStmtsMap,AllDstValTokenMap);
                srcPathToMatcher.put(srcFilePath,matcher);
            }catch (Exception e){
                e.printStackTrace();
                this.srcFilePath = null;
                this.dstFilePath = null;
                throw new RuntimeException(e.getMessage());
            }
        }


        for (String srcToPath : srcStmtsToMap.keySet()){
            srcPath = srcToPath;
            List<ProgramElement> srcStmts = new ArrayList<>();
            srcStmts = srcStmtsToMap.get(srcToPath);
//            iASTMapper mc = srcPathToMatcher.get(srcToPath);
            matcher.buildMappingsOuterLoop(srcStmts, this.srcFilePath,dstFilePath);//执行外层循环，建立元素映射及节点映射
            this.eleMappings = matcher.getEleMappings();
            srcRootEle = matcher.getSrcRootEle();
//            dstRootEle = matcher.getDstRootEle();
            calMappingRecords();
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

    /**
     * If file is only renamed, not necessary to analyze it.
     */
    private boolean checkOnlyRenameOperation(String project, String baseCommitId,
                                             String commitId, String oldFilePath,
                                             String newFilePath) {
        try {
            String oldContent = GitUtils
                    .getFileContentOfCommitFile(project, baseCommitId, oldFilePath)
                    .toString("UTF-8");
            String newContent = GitUtils
                    .getFileContentOfCommitFile(project, commitId, newFilePath)
                    .toString("UTF-8");
            return oldContent.equals(newContent);
        } catch (Exception e){
            throw new RuntimeException("cannot retrieve file content");
        }
    }

    /**
     * If not add or delete code lines, not necessary to analyze it.
     */
    private boolean checkAddedOrDeletedLines(String srcFilePath, String dstFilePath){
        Set<Integer> addedLines = GitHunk.getAllAddedLines(project, commitId, srcFilePath, false);
        Set<Integer> deletedLines = GitHunk.getAllDeletedLines(project, commitId, srcFilePath, false);

        boolean nonAddedLines = addedLines == null || addedLines.size() == 0;
        boolean nonDeletedLines = deletedLines == null || deletedLines.size() == 0;

        return nonAddedLines && nonDeletedLines;
    }

    public String getSrcFilePath() {
//        return srcFilePath;
        return srcPath;
    }

    public String getDstFilePath() {
        return dstFilePath;
    }

    public iASTMapper getMatcher(String srcPath) {
        return srcPathToMatcher.get(srcPath);
    }
}
