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
    public static Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap;


    public CommitAnalysis(String project, String commitId, boolean stmtOrToken) {
        this.project = project;
        this.commitId = commitId;
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
        this.AllSrcPathToStmtsMap = new HashMap<>();
        allDstStmts = new ArrayList<>();
    }

    public void calResultMapping(boolean doComparison, boolean doEvaluation) {
        String baseCommitId = GitUtils.getBaseCommitId(project, commitId);
        if (baseCommitId == null)
            return;
        Map<String, String> pathMap = GitInfoRetrieval.getOldModifiedFileMap(project, commitId);//获取“原文件-修订文件”对
        if (pathMap == null || pathMap.size() == 0)
            return;

        for (String srcFilePath : pathMap.keySet()){
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
                matcher.preStoreAllDstCandidates(srcFilePath, dstFilePath, AllDstStmtsToMap, AllDstTokensToMap,
                        AllDstinnerStmtsToMap,AllDstPathToStmtsMap, AllDstPathToTokensMap, AllDstPathToinnerStmtsMap,
                        AllDstValTokenMap, AllSrcPathToStmtsMap);
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
                    RevisionAnalysis result = new RevisionAnalysis(project, commitId, srcToPath, pathMap,mc, srcPathToMatcher,
                            srcStmts, AllSrcPathToStmtsMap);
                    resultMap.put(srcToPath, result);
                }
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
