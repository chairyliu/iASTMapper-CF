package cs.model.analysis;

import com.github.gumtreediff.tree.ITree;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.iASTMapper;
import cs.model.algorithm.utils.GumTreeUtil;
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
    private ITree src;
    private ITree dst;
    private iASTMapper matcher;

    public static Map<String, List<ProgramElement>> srcPathToStmtsMap;
    public static Map<String, List<ProgramElement>> dstPathToStmtsMap;
    public static Map<String, ProgramElement> dstPathToRoot;
    public static Map<String, ProgramElement> srcPathToRoot;
    public static List<ProgramElement> AllDstStmtsList;
    public static List<ProgramElement> AllDstTokensList;
    public static List<ProgramElement> AllDstinnerStmtsList;
    public static Map<String, Set<ProgramElement>> AllDstPathToStmtsMap;
    public static Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap;
    public Map<String, Set<ProgramElement>> AllDstValTokenMap;
    public Map<String, iASTMapper> srcPathToMatcher = new HashMap<>();
    public List<ProgramElement> allDstStmts;
    private long treeBuildTime;

    public CommitAnalysis(String project, String commitId, boolean stmtOrToken) {
        this.project = project;
        this.commitId = commitId;
        this.resultMap = new HashMap<>();
        this.comparisonResultMap = new HashMap<>();
        this.evaluationMap = new HashMap<>();
        this.stmtOrToken = stmtOrToken;
        srcPathToStmtsMap = new HashMap<>();
        dstPathToStmtsMap = new HashMap<>();
        dstPathToRoot = new HashMap<>();
        srcPathToRoot = new HashMap<>();
        this.AllDstPathToStmtsMap = new HashMap<>();
        this.AllDstStmtsList = new ArrayList<>();
        this.AllDstTokensList = new ArrayList<>();
        this.AllDstinnerStmtsList = new ArrayList<>();
        this.AllDstValTokenMap = new HashMap<>();
        this.AllSrcPathToStmtsMap = new HashMap<>();
        allDstStmts = new ArrayList<>();
    }

    public void calResultMapping(boolean doCrossFileMapping) {
        String baseCommitId = GitUtils.getBaseCommitId(project, commitId);
        if (baseCommitId == null)
            return;
        Map<String, String> pathMap = GitInfoRetrieval.getOldModifiedFileMap(project, commitId);
        if (pathMap == null || pathMap.size() == 0)
            return;
        boolean isSingleFile = false;
        if (pathMap.size() == 1){
            isSingleFile = true;
        }

        for (String srcFilePath : pathMap.keySet()){
            String dstFilePath = pathMap.get(srcFilePath);
            if (dstFilePath == null)
                continue;
            if (checkOnlyRenameOperation(project, baseCommitId, commitId, srcFilePath, dstFilePath))
                continue;
            if (checkAddedOrDeletedLines(srcFilePath))
                continue;

            try {
                // get file content
                ByteArrayOutputStream srcFileStream = GitUtils
                        .getFileContentOfCommitFile(project, baseCommitId, srcFilePath);
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

                // get the AST tree
                long time1 = System.currentTimeMillis();
                src = GumTreeUtil.getITreeRoot(srcFileContent, "gt");
                dst = GumTreeUtil.getITreeRoot(dstFileContent, "gt");
                long time2 = System.currentTimeMillis();
                treeBuildTime = time2 - time1;
                if (src == null || dst == null)
                    return;

                matcher = new iASTMapper(srcFileContent, dstFileContent, src, dst, srcFilePath, dstFilePath, pathMap,
                        srcPathToStmtsMap, dstPathToStmtsMap, dstPathToRoot, srcPathToRoot, allDstStmts);
                matcher.identicalMapping();
                if (doCrossFileMapping)
                    matcher.preStoreAllDstCandidates(srcFilePath, dstFilePath, AllDstStmtsList, AllDstTokensList,
                            AllDstinnerStmtsList,AllDstPathToStmtsMap, AllDstValTokenMap, AllSrcPathToStmtsMap);
                srcPathToMatcher.put(srcFilePath,matcher);
            }catch (Exception e){
                e.printStackTrace();
                this.srcFilePath = null;
                this.dstFilePath = null;
                throw new RuntimeException(e.getMessage());
            }
        }

        try {
            // For each source file, iterate through all target files in the same commit
            for (String srcToPath : srcPathToStmtsMap.keySet()){
                List<ProgramElement> srcStmts = new ArrayList<>();
                srcStmts = srcPathToStmtsMap.get(srcToPath);
                if (srcStmts == null)
                    return;
                iASTMapper mc = srcPathToMatcher.get(srcToPath);
                RevisionAnalysis result = new RevisionAnalysis(project, commitId, srcToPath, pathMap, mc, srcPathToMatcher,
                        srcStmts, AllSrcPathToStmtsMap, isSingleFile, doCrossFileMapping);
                resultMap.put(srcToPath, result);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Map<String, RevisionAnalysis> getRevisionAnalysisResultMap() { return resultMap;}

    public Map<String, RevisionComparison> getComparisonResultMap() { return comparisonResultMap;}

    public Map<String, RevisionEvaluation> getEvaluationMap() { return evaluationMap;}

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
    private boolean checkAddedOrDeletedLines(String srcFilePath){
        Set<Integer> addedLines = GitHunk.getAllAddedLines(project, commitId, srcFilePath, false);
        Set<Integer> deletedLines = GitHunk.getAllDeletedLines(project, commitId, srcFilePath, false);

        boolean nonAddedLines = addedLines == null || addedLines.size() == 0;
        boolean nonDeletedLines = deletedLines == null || deletedLines.size() == 0;

        return nonAddedLines && nonDeletedLines;
    }
}
