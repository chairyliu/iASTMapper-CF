package cs.model.analysis;

import com.github.gumtreediff.tree.ITree;
import cs.model.algorithm.element.ElementTreeUtils;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.iASTMapper;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.utils.GumTreeUtil;
import cs.model.baseline.BaselineMatcher;
import cs.model.evaluation.config.MyConfig;
import cs.model.evaluation.csvrecord.compare.ComparisonRecord;
import cs.model.evaluation.csvrecord.compare.StmtComparisonRecord;
import cs.model.evaluation.csvrecord.compare.TokenComparisonRecord;
import cs.model.evaluation.utils.PairwiseComparison;
import cs.model.gitops.GitUtils;

import java.io.ByteArrayOutputStream;
import java.util.*;

public class RevisionComparison {
    private String project;
    private String commitId;
    private String srcFilePath;
    private iASTMapper myMatcher;

    private ProgramElement srcRootEle;
    private ProgramElement dstRootEle;
    private ElementMappings myMappings;
    private ElementMappings gtMappings;
    private ElementMappings mtdMappings;
    private ElementMappings ijmMappings;

    private Set<ProgramElement> inconsistentlyMappedElements;
    private Set<ProgramElement> inconsistentlyMappedElementsWithGt;
    private Set<ProgramElement> inconsistentlyMappedElementsWithMtd;
    private Set<ProgramElement> inconsistentlyMappedElementsWithIjm;

    private String script;
    private List<String[]> records;
    private boolean noGoodResult = false;
    private boolean stmtOrToken;

    private List<ProgramElement> srcElements;
    private List<ProgramElement> dstElements;
    public Map<String, List<ProgramElement>> srcStmtsToMap;
    public Map<String, List<ProgramElement>> dstStmtsToMap;
    public List<ProgramElement> allDstStmts;
    public static Map<String, ProgramElement> dstPathToRoot;
    public Map<String, ProgramElement> srcPathToRoot;
    protected Map<String, String> pathMap;
    public static Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap;
    private ITree src;
    private ITree dst;

    public RevisionComparison(String project, String commitId, String baseCommitId,
                              String srcFilePath, String dstFilePath, boolean stmtOrToken, boolean isSingleFile) throws Exception {
        this.project = project;
        this.commitId = commitId;
        this.srcFilePath = srcFilePath;
        this.stmtOrToken = stmtOrToken;
        this.srcElements = new ArrayList<>();
        this.dstElements = new ArrayList<>();
        this.inconsistentlyMappedElements = new HashSet<>();

        // get source file content
        ByteArrayOutputStream srcFileStream = GitUtils
                .getFileContentOfCommitFile(project, baseCommitId, srcFilePath);
        String srcFileContent = srcFileStream.toString("UTF-8");
        if (srcFileContent.equals("")) {
            noGoodResult = true;
            return;
        }

        // get target file content
        ByteArrayOutputStream dstFileStream = GitUtils
                .getFileContentOfCommitFile(project, commitId, dstFilePath);
        String dstFileContent = dstFileStream.toString("UTF-8");
        if (dstFileContent.equals("")) {
            noGoodResult = true;
            return;
        }
        src = GumTreeUtil.getITreeRoot(srcFileContent, "gt");
        dst = GumTreeUtil.getITreeRoot(dstFileContent, "gt");
        // build mappings using our method
        this.myMatcher = new iASTMapper(srcFileContent, dstFileContent,src,dst, srcFilePath, dstFilePath, pathMap,srcStmtsToMap, dstStmtsToMap,dstPathToRoot, srcPathToRoot, allDstStmts);
        //下三行新增-ljy
//        srcStmtsToMap = myMatcher.getSrcStmtsToMap();
        List<ProgramElement> srcStmts = new ArrayList<>();
        srcStmts = srcStmtsToMap.get(srcFilePath);
        this.myMatcher.buildMappingsOuterLoop(srcStmts, srcFilePath, pathMap, AllSrcPathToStmtsMap, isSingleFile, true);
        this.myMappings = this.myMatcher.getEleMappings();

        // build mappings using gumtree, mtdiff and ijm
        BaselineMatcher baselineAnalysis = new BaselineMatcher(myMatcher);
        this.gtMappings = baselineAnalysis.getOriginalMethodMapping("gt");
        this.mtdMappings = baselineAnalysis.getOriginalMethodMapping("mtdiff");
        this.ijmMappings = baselineAnalysis.getOriginalMethodMapping("ijm");

        this.srcRootEle = this.myMatcher.getSrcRootEle();
        this.dstRootEle = this.myMatcher.getDstRootEle();
        initAnalyzedElements();
        initMappingComparison();
    }

    public boolean isNoGoodResult() {
        return noGoodResult;
    }

    public String getScript() {
        return script;
    }

    public List<String[]> getRecords() {
        return records;
    }

    private void initAnalyzedElements(){
        if (stmtOrToken) {
            srcElements = ElementTreeUtils.getAllStmtsPreOrder(srcRootEle);
            dstElements = ElementTreeUtils.getAllStmtsPreOrder(dstRootEle);
        } else {
            List<ProgramElement> srcStmts = ElementTreeUtils.getAllStmtsPreOrder(srcRootEle);
            List<ProgramElement> dstStmts = ElementTreeUtils.getAllStmtsPreOrder(dstRootEle);
            for (ProgramElement stmt: srcStmts)
                srcElements.addAll(stmt.getTokenElements());
            for (ProgramElement stmt: dstStmts)
                dstElements.addAll(stmt.getTokenElements());
        }
    }

    private void initMappingComparison(){
        PairwiseComparison comparison1 = new PairwiseComparison(myMappings, gtMappings, srcElements, dstElements);
        PairwiseComparison comparison2 = new PairwiseComparison(myMappings, mtdMappings, srcElements, dstElements);
        PairwiseComparison comparison3 = new PairwiseComparison(myMappings, ijmMappings, srcElements, dstElements);

        inconsistentlyMappedElementsWithGt = comparison1.getInconsistentlyMappedElements();
        inconsistentlyMappedElementsWithMtd = comparison2.getInconsistentlyMappedElements();
        inconsistentlyMappedElementsWithIjm = comparison3.getInconsistentlyMappedElements();

        inconsistentlyMappedElements.addAll(inconsistentlyMappedElementsWithGt);
        inconsistentlyMappedElements.addAll(inconsistentlyMappedElementsWithMtd);
        inconsistentlyMappedElements.addAll(inconsistentlyMappedElementsWithIjm);
        generateRecordsAndScript();
    }

    private void generateRecordsAndScript(){
        script = "";
        records = new ArrayList<>();

        script += "URL: " + MyConfig.getCommitUrl(project, commitId) + "\n";
        script += "File Path: " + srcFilePath + "\n";
        script += "===============================================================\n";

        Set<ProgramElement> addedDstElements = new HashSet<>();
        for (ProgramElement ele: srcElements){
            if (inconsistentlyMappedElements.contains(ele)){
                ProgramElement dstEle = myMappings.getDstForSrc(ele);
                ComparisonRecord cr;

                boolean gtInconsistent = inconsistentlyMappedElementsWithGt.contains(ele);
                boolean mtdInconsistent = inconsistentlyMappedElementsWithMtd.contains(ele);
                boolean ijmInconsistent = inconsistentlyMappedElementsWithIjm.contains(ele);

                if (stmtOrToken)
                    cr = new StmtComparisonRecord(project, commitId, srcFilePath,
                            gtInconsistent, mtdInconsistent, ijmInconsistent, ele, dstEle);
                else
                    cr = new TokenComparisonRecord(project, commitId, srcFilePath,
                            gtInconsistent, mtdInconsistent, ijmInconsistent, ele, dstEle);
                String[] record = cr.toRecord(myMappings);
                if (record != null) {
                    records.add(record);
                    script += cr.getScript() + "\n";
                }
                if (dstEle != null)
                    addedDstElements.add(dstEle);
            }
        }

        for (ProgramElement ele: dstElements){
            if (addedDstElements.contains(ele))
                continue;
            boolean gtInconsistent = inconsistentlyMappedElementsWithGt.contains(ele);
            boolean mtdInconsistent = inconsistentlyMappedElementsWithMtd.contains(ele);
            boolean ijmInconsistent = inconsistentlyMappedElementsWithIjm.contains(ele);
            if (inconsistentlyMappedElements.contains(ele)){
                ComparisonRecord cr;
                if (stmtOrToken)
                    cr = new StmtComparisonRecord(project, commitId, srcFilePath,
                            gtInconsistent, mtdInconsistent, ijmInconsistent,
                            null, ele);
                else
                    cr = new TokenComparisonRecord(project, commitId, srcFilePath,
                            gtInconsistent, mtdInconsistent, ijmInconsistent,
                            null, ele);
                String[] record = cr.toRecord(myMappings);
                records.add(record);
                script += cr.getScript() + "\n";
            }
        }
    }

}
