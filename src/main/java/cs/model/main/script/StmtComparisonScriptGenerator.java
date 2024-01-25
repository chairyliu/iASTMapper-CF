package cs.model.main.script;

import cs.model.analysis.CommitAnalysis;
import cs.model.analysis.RevisionComparison;
import cs.model.evaluation.csvrecord.compare.StmtComparisonRecord;
import cs.model.main.analysis.StmtMappingCompareAnalysis;
import cs.model.utils.CsvOperationsUtil;
import cs.model.utils.FileRevision;
import cs.model.utils.PathResolver;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

/**
 * Generate script to compare the stmt mappings as generated by SE-Mapping and other methods.
 */
public class StmtComparisonScriptGenerator {

    private static String project;
    private static int sampleNum = 10;

    public static void generate(String project, int sampleNum) throws Exception {
        StmtComparisonScriptGenerator.project = project;
        StmtComparisonScriptGenerator.sampleNum = sampleNum;
        run();
    }

    private static void run() throws Exception {
        List<String[]> csvRecords = StmtMappingCompareAnalysis.getRecords(project, false);
        List<FileRevision> revisions = StmtComparisonRecord.getAllFileRevisionsFromCsvRecords(csvRecords);
        Collections.shuffle(revisions, new Random(1));
        List<FileRevision> sampledRevisions = revisions.subList(0, sampleNum);
        doMappingAndGenerateCsvScript(sampledRevisions);
    }

    private static void doMappingAndGenerateCsvScript(List<FileRevision> sampledRevisions) throws Exception {
        int idx = 1;
        List<String[]> allRecords = new ArrayList<>();
        for (FileRevision fr: sampledRevisions){
            String commitId = fr.first;
            String filePath = fr.second;
            Set<String> filesToAnalyze = new HashSet<>();
            filesToAnalyze.add(filePath);

            CommitAnalysis commitResult = new CommitAnalysis(project, commitId, filesToAnalyze, true);
            commitResult.calResultMappings(true, false);

            Map<String, RevisionComparison> comparisonMap = commitResult.getComparisonResultMap();
            RevisionComparison comparison = comparisonMap.get(filePath);
            List<String[]> tmpRecords = comparison.getRecords();

            if (tmpRecords.size() <= 50) {
                String script = comparison.getScript();
                allRecords.addAll(tmpRecords);
                writeScript(script, idx);
                idx++;
            }

            if (idx == sampleNum + 1)
                break;
        }

        String[] header = StmtComparisonRecord.getHeaders();
        String csvPath = PathResolver.getStmtMappingManualAnalysisCsvPath(project);
        CsvOperationsUtil.writeCSV(csvPath, header, allRecords);
    }

    private static void writeScript(String script, int idx) throws Exception {
        String scriptPath = PathResolver.getStmtMappingManualAnalysisScriptPath(project, idx);
        FileUtils.writeStringToFile(new File(scriptPath), script, "UTF-8");
    }

}
