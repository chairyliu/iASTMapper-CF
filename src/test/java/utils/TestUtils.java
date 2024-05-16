package utils;

import cs.model.algorithm.actions.StmtTokenAction;
import cs.model.algorithm.actions.TreeEditAction;
import cs.model.analysis.CommitAnalysis;
import cs.model.analysis.RevisionAnalysis;
import cs.model.baseline.BaselineMatcher;
import cs.model.gitops.GitInfoRetrieval;
import cs.model.gitops.GitUtils;
import cs.model.utils.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class TestUtils {
    public static void testiASTMapper(String project, String commitId) throws IOException {
        CommitAnalysis mappingResult = new CommitAnalysis(project, commitId, false);
        long time1 = System.currentTimeMillis();
        mappingResult.calResultMapping(false, false);
        Map<String, RevisionAnalysis> resultMap = mappingResult.getRevisionAnalysisResultMap();

        for (String filePath: resultMap.keySet()){
            System.out.println(filePath);

            RevisionAnalysis m = resultMap.get(filePath);

            List<StmtTokenAction> actionList = m.generateActions();
            List<TreeEditAction> treeEditActions = m.generateEditActions();

            for (StmtTokenAction action: actionList)//语句编辑操作
                System.out.println(action);

            for (TreeEditAction action: treeEditActions) {//AST编辑操作
                System.out.println(action);
            }

            System.out.println("action num: " + treeEditActions.size());
        }
        long time2 = System.currentTimeMillis();
        System.out.println(time2 - time1);
    }

    public static void testBaseline(String project, String commitId, String file, String method) throws Exception {
        long time1 = System.currentTimeMillis();
        Pair<String, String> contentPair = getSrcAndDstFileContent(project, commitId, file);
        if (contentPair == null)
            return;
        String srcFileContent = contentPair.first;
        String dstFileContent = contentPair.second;

        BaselineMatcher matcher = new BaselineMatcher(srcFileContent, dstFileContent);
        List<StmtTokenAction> actions = matcher.generateStmtTokenEditActionsForMethod(method);
        List<TreeEditAction> treeEditActions = matcher.getTreeEditActions();

        for (StmtTokenAction action: actions)
            System.out.println(action);

        for (TreeEditAction action: treeEditActions)
            System.out.println(action);

        long time2 = System.currentTimeMillis();
        System.out.println(time2 - time1);
        System.out.println("action num: " + treeEditActions.size());
    }

    public static Pair<String, String> getSrcAndDstFileContent(String project,
                                                                String commitId,
                                                                String srcFilePath) throws UnsupportedEncodingException {
        String baseCommitId = GitUtils.getBaseCommitId(project, commitId);
        if (baseCommitId == null)
            return null;
        Map<String, String> pathMap = GitInfoRetrieval.getOldModifiedFileMap(project, commitId);
        if (pathMap == null || pathMap.size() == 0)
            return null;
        String dstPath = pathMap.get(srcFilePath);

        ByteArrayOutputStream srcFileStream = GitUtils.getFileContentOfCommitFile(project, baseCommitId, srcFilePath);
        String srcFileContent = srcFileStream.toString("UTF-8");

        ByteArrayOutputStream dstFileStream = GitUtils.getFileContentOfCommitFile(project, commitId, dstPath);
        String dstFileContent = dstFileStream.toString("UTF-8");

        return new Pair<>(srcFileContent, dstFileContent);
    }
}
