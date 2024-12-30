package resultAnalyzer;

import com.github.gumtreediff.matchers.MappingStore;
import cs.model.algorithm.actions.StmtTokenAction;
import cs.model.algorithm.actions.TreeEditAction;
import cs.model.algorithm.iASTMapper;
import cs.model.analysis.CommitAnalysis;
import cs.model.analysis.RevisionAnalysis;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;


public class iASTMapper_runner {

    public static void run(String project_commits_path, String method_errors_file, String resPath){

        Set<String> erroredFRs = new HashSet<>();
        if (new File(method_errors_file).exists())
            erroredFRs = iASTMapper_runner.getErroredFRs(method_errors_file);

        Set<String> filePaths = new HashSet<>();
        String previousCommitId = null;

        try {

            BufferedWriter ebw = new BufferedWriter(new FileWriter(method_errors_file, true));

            File pfo = new File(project_commits_path);
            for (File f : pfo.listFiles()) {
                if(f.isFile()) {
                    String fn = f.getName();
                    if (!fn.endsWith(".txt"))
                        continue;

                    String project = fn.replace(".txt", "");
                    String projectPath = resPath + "\\" + project;
                    File fo = new File(projectPath);
                    if (!fo.exists())
                        fo.mkdirs();
                    String subProject = "one-to-one mapping";
                    String subProjectPath = projectPath + File.separator + subProject;
                    File subFo = new File(subProjectPath);
                    if (!subFo.exists())
                        subFo.mkdirs();
                    Set<String> analyzedFRs = new HashSet<>();
                    String project_resFile = resPath + "\\" + project + ".txt";
                    if (new File(project_resFile).exists()) {
                        analyzedFRs = getAnalyzedFRs(project_resFile);
                        System.out.println("#Analyzed FRs: " + analyzedFRs.size());
                    }

                    String line;
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(project_resFile, true));
                    BufferedWriter bw1;
                    BufferedWriter bw2;
                    BufferedWriter bw3;
                    while((line = br.readLine())!=null) {
                        String[] sa = line.split(" ");
                        if (sa.length == 4) {
                            String commitId = sa[0];
                            String oldPath = sa[2];
                            String FR = commitId + " " + oldPath;
                            if (analyzedFRs.contains(FR) || erroredFRs.contains(FR))
                                continue;

                            if (previousCommitId != null) {
                                if (previousCommitId.equals(commitId))
                                    continue;
                            }
                            previousCommitId = commitId;
                            CommitAnalysis mappingResult = null;
                            long time = 0;
                            ExecutorService executor = null;
                            Future<Object[]> future = null;

                            try{
                                executor = Executors.newSingleThreadExecutor(r -> {
                                    Thread t = new Thread(r);
                                    t.setUncaughtExceptionHandler((thread, e) -> {
                                        System.out.println("Unhandled exception: " + e);
                                        e.printStackTrace();
                                    });
                                    return t;
                                });
                                filePaths.clear();
                                filePaths.add(oldPath);
                                future = executor.submit(() -> {
                                    CommitAnalysis result = new CommitAnalysis(project, commitId,false);
                                    long time1 = System.currentTimeMillis();
                                    result.calResultMapping(true);
                                    long time2 = System.currentTimeMillis();
                                    long executionTime = time2 - time1;
                                    return new Object[]{result, executionTime};
                                });

                                Object[] results = future.get(5, TimeUnit.MINUTES);
                                if (results != null && results.length > 0){
                                    mappingResult = (CommitAnalysis) results[0];
                                    time = (Long) results[1];
                                } else {
                                    throw new IllegalStateException("Invalid results from future");
                                }
                            } catch (TimeoutException te) {
                                if (future != null)
                                    future.cancel(true);
                                if (ebw != null){
                                    ebw.write(commitId + " " + oldPath + " -> Timeout\n");
                                    ebw.flush();
                                }
                                continue;
                            } catch (InterruptedException | ExecutionException e) {
                                if (ebw != null){
                                    ebw.write(commitId + " " + oldPath + " -> " + e.getMessage() + "\n");
                                    ebw.flush();
                                }
                                continue;
                            } finally {
                                if (executor != null)
                                    executor.shutdown();
                            }
                            try {
                                if (mappingResult != null) {
                                    bw1 = new BufferedWriter(new FileWriter(
                                            subProjectPath + "\\" + commitId + ".txt", true));
                                    bw2 = new BufferedWriter(new FileWriter(
                                            projectPath + "\\" + "cross-file mapping" + ".txt", true));
                                    bw3 = new BufferedWriter(new FileWriter(
                                            projectPath + "\\" + "one-to-multi mapping" + ".txt", true));
                                    Map<String, RevisionAnalysis> resultMap = mappingResult.getRevisionAnalysisResultMap();

                                    if (resultMap.size() == 0) {
                                        /**
                                         * If the content of oldPath or newPath is "" (e.g., no longer exist)
                                         * OR there is runtime error happened during the execution, then the resultMap is empty!
                                         */
                                        ebw.write(commitId + " " + oldPath + " -> No Result!\n");
                                        ebw.flush();
                                        continue;
                                    }

                                    for (String filePath : resultMap.keySet()) {
                                        RevisionAnalysis m = resultMap.get(filePath);
                                        MappingStore ms = m.getMatcher().getMs();
                                        List<StmtTokenAction> actionList = m.generateActions(filePath);
                                        List<TreeEditAction> treeEditActions = m.generateEditActions();

                                        bw1.write("\n\nFile: " + filePath + "\n\n");
                                        bw1.write("************* Code Edit Script ***************\n");
                                        for (StmtTokenAction action : actionList)
                                            bw1.write(action.toString());

                                        bw1.write("\n\n************* AST Edit Script ***************\n");
                                        for (TreeEditAction action : treeEditActions)
                                            bw1.write(action.toString());

                                        iASTMapper matcher = m.getMatcher();
                                        Map<Map<String, String>, String> crossFileMap = matcher.getCrossFileMap();
                                        if (crossFileMap.size() != 0) {
                                            bw2.write("\ncommitId: " + commitId + "\n");
                                            for (Map<String, String> pathToSrcStmt : crossFileMap.keySet()) {
                                                String dstStmt = crossFileMap.get(pathToSrcStmt);
                                                for (String path : pathToSrcStmt.keySet()) {
                                                    String srcStmt = pathToSrcStmt.get(path);
                                                    String[] parts = path.split("\\+");
                                                    bw2.write("srcFile: " + parts[0] + "\n");
                                                    bw2.write("dstFile: " + parts[1] + "\n");
                                                    bw2.write(srcStmt + " —> " + dstStmt + "\n\n");
                                                }
                                            }
                                        }

                                        Map<String, List<String>> oneToMultiMap = matcher.getOneToMultiMap();
                                        if (oneToMultiMap.size() != 0){
                                            bw3.write("\ncommitId: " + commitId + "\n");
                                            for (String srcPathAndMappingPair : oneToMultiMap.keySet()){
                                                List<String> dstPathes = oneToMultiMap.get(srcPathAndMappingPair);
                                                String[] parts = srcPathAndMappingPair.split("\\+");
                                                bw3.write("srcFile: " + parts[0] + "\n");
                                                for (String dstPath : dstPathes){
                                                    bw3.write("dstFile: " + dstPath + "\n");
                                                }
                                                bw3.write(parts[1] + "\n\n");
                                            }
                                        }
                                        int ASTNodeMappings_num = ms.size();
                                        int eleMappings_num = m.getMatcher().getEleMappings().asSet().size();
                                        int ASTESSize = treeEditActions.size();
                                        int CodeESSize = actionList.size();
                                        String record = commitId + " " + filePath + " -> " +
                                                ASTNodeMappings_num + " " + eleMappings_num + " " + ASTESSize +
                                                " " + CodeESSize + " " + time;
                                        System.out.println(record);
                                        bw.write(record + "\n");
                                        bw1.flush();
                                        bw2.flush();
                                        bw3.flush();
                                        bw.flush();
                                    }
                                    bw1.close();
                                    bw2.close();
                                    bw3.close();
                                }
                            } catch(Exception e){
                                ebw.write(commitId + " " + oldPath + " -> " + e.getMessage() + "\n");
                                ebw.flush();
                            }
                        }
                    }
                    br.close();
                    bw.close();
                }
            }
            ebw.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the set of analyzed file revisions.
     */
    public static Set<String> getAnalyzedFRs(String project_resFile) {

        Set<String> FRs = new HashSet<>();

        try {
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(project_resFile));
            while((line = br.readLine())!=null) {
                if (line.contains("->"))
                    FRs.add(line.split("->")[0].trim());
            }
            br.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return FRs;
    }

    /**
     * Get the set of file revisions that had analysis errors.
     */
    public static Set<String> getErroredFRs(String project_errorFile) {

        Set<String> FRs = new HashSet<>();

        try {
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(project_errorFile));
            while((line = br.readLine())!=null) {
                if (line.contains("->"))
                    FRs.add(line.split("->")[0].trim());
            }
            br.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return FRs;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {

        String project_commits_path = "D:\\iASTMapper\\tse\\project_commits-1";

        String iASTMapper_resPath = "D:\\iASTMapper\\tse\\iASTMapper_res";
        String iASTMapper_errorsFile = iASTMapper_resPath + "\\__errors_.txt";
        GitProjectInfoExtractor.createPath(iASTMapper_resPath);
        iASTMapper.used_ASTtype = "gt";
        run(project_commits_path, iASTMapper_errorsFile, iASTMapper_resPath);  // run iASTMapper
    }

}
