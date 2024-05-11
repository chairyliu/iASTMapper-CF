package cs.model.analysis;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.iASTMapper;
import cs.model.gitops.GitHunk;
import cs.model.gitops.GitInfoRetrieval;
import cs.model.gitops.GitUtils;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Perform analysis for a commit.
 *
 * This class provides three ways to analyze a commit
 * 1. common use of tool: to analyze a commit using our algorithm.
 * 2. do comparison: compare mappings of statements and tokens by our algorithm with other algorithms
 * 3. do evaluation: similar to 2, but combine the results of comparison for statements and tokens
 */
public class CommitAnalysis {
    private String project;
    private String commitId;
    private Set<String> filesToAnalyze;
    private Map<String, RevisionAnalysis> resultMap;
    private Map<String, RevisionComparison> comparisonResultMap;
    private Map<String, RevisionEvaluation> evaluationMap;
    private boolean stmtOrToken;

    protected String srcFilePath;
    protected String dstFilePath;
    protected String srcFileContent;
    protected String dstFileContent;
    protected Map<String, String> pathMap;
    private iASTMapper matcher;

    public static Map<String, List<ProgramElement>> srcStmtsToMap;
    public static Map<String, ProgramElement> dstPathToRoot;
    public static Map<String, ProgramElement> srcPathToRoot;
    public static List<ProgramElement> AllDstStmtsToMap;
    public static List<ProgramElement> AllDstTokensToMap;
    public static List<ProgramElement> AllDstinnerStmtsToMap;
    public static Map<String, Set<ProgramElement>> AllDstPathToStmtsMap;
    public static Map<String, Set<ProgramElement>> AllDstPathToTokensMap;
    public static Map<String, Set<ProgramElement>> AllDstPathToinnerStmtsMap;
    public Map<String, Set<ProgramElement>> AllDstValTokenMap;
    public Map<String, iASTMapper> srcPathToMatcher = new HashMap<>();
    public List<ProgramElement> allDstStmts;


    public CommitAnalysis(String project, String commitId, boolean stmtOrToken) {//如果是跨文件，这里可以删去filesToAnalyze
        this.project = project;
        this.commitId = commitId;
//        this.filesToAnalyze = filesToAnalyze;//删，因为后面的pathMap可以获取到每个commitId下的所以file路径对，原来的一对一需要将pathMap筛选到只分析当前的文件，所以设置了这样一个参数
        this.resultMap = new HashMap<>();
        this.comparisonResultMap = new HashMap<>();//后期去掉
        this.evaluationMap = new HashMap<>();//后期去掉
        this.stmtOrToken = stmtOrToken;

        srcStmtsToMap = new HashMap<>();
        dstPathToRoot = new HashMap<>();
        srcPathToRoot = new HashMap<>();
        this.AllDstPathToStmtsMap = new HashMap<>();
        this.AllDstPathToTokensMap = new HashMap<>();
        this.AllDstPathToinnerStmtsMap = new HashMap<>();
        this.AllDstStmtsToMap = new ArrayList<>();
        this.AllDstTokensToMap = new ArrayList<>();
        this.AllDstinnerStmtsToMap = new ArrayList<>();
        this.AllDstValTokenMap = new HashMap<>();
        allDstStmts = new ArrayList<>();
    }

    public void calResultMapping(boolean doComparison, boolean doEvaluation) {//可以获取pathMap后将整个map传入RevisionAnalysis中
        String baseCommitId = GitUtils.getBaseCommitId(project, commitId);//提到进入这个函数之前
        if (baseCommitId == null)
            return;
        Map<String, String> pathMap = GitInfoRetrieval.getOldModifiedFileMap(project, commitId);//获取“原文件-修订文件”对
        if (pathMap == null || pathMap.size() == 0)
            return;

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
                matcher = new iASTMapper(srcFileContent, dstFileContent, srcFilePath, dstFilePath,
                        srcStmtsToMap, dstPathToRoot, srcPathToRoot,allDstStmts);//创建iASTMapper对象
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

        try {
            if (doComparison){//去掉
                /*
                  Case 1:
                  Separately compare mappings of statements and tokens.
                 */
//                    RevisionComparison comparison = new RevisionComparison(project, commitId, baseCommitId,
//                            oldPath, newPath, stmtOrToken);
//                    if (!comparison.isNoGoodResult())
//                        comparisonResultMap.put(oldPath, comparison);
            } else if (doEvaluation) {//删去
//                    System.out.println(oldPath);
                /*
                  Case 2:
                  Compare mappings of statements and tokens collectively.
                 */
//                    RevisionEvaluation comparison = new RevisionEvaluation(project, commitId, baseCommitId,
//                            oldPath, newPath);
//                    if (!comparison.isNoGoodResult())
//                        evaluationMap.put(oldPath, comparison);
            } else {
                /*
                  Case 3:
                  directly analyze mappings generated by our method.
                 */
                for (String srcToPath : srcStmtsToMap.keySet()){
                    List<ProgramElement> srcStmts = new ArrayList<>();
                    srcStmts = srcStmtsToMap.get(srcToPath);
                    iASTMapper mc = srcPathToMatcher.get(srcToPath);
                    RevisionAnalysis result = new RevisionAnalysis(project, commitId, srcToPath, pathMap,mc, srcPathToMatcher, srcStmts);
                    resultMap.put(srcToPath, result);
                }
//                RevisionAnalysis result = new RevisionAnalysis(project, commitId, baseCommitId,
//                        pathMap);//建立oldPath与newPath之间的映射结果
//                if (result.getSrcFilePath() != null && result.getDstFilePath() != null){
//                String oldPath = result.getSrcFilePath();//修改输出srcPath的位置
//                resultMap.put(oldPath, result);//并将oldPath的结果存于resultMap中
//                    }

//                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Map<String, RevisionAnalysis> getRevisionAnalysisResultMap() {
        return resultMap;
    }

    public Map<String, RevisionComparison> getComparisonResultMap() {
        return comparisonResultMap;
    }

    public Map<String, RevisionEvaluation> getEvaluationMap() {
        return evaluationMap;
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
}
