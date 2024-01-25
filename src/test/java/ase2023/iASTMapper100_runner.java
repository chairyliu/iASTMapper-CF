package ase2023;

import com.github.gumtreediff.matchers.MappingStore;
import cs.model.algorithm.actions.StmtTokenAction;
import cs.model.algorithm.actions.TreeEditAction;
import cs.model.algorithm.iASTMapper;
import cs.model.analysis.CommitAnalysis;
import cs.model.analysis.RevisionAnalysis;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class iASTMapper100_runner {


    public static void run(String project_commits_path, String method_errors_file, String resPath){

        Set<String> erroredFRs = new HashSet<>();
        if (new File(method_errors_file).exists())
            erroredFRs = iASTMapper100_runner.getErroredFRs(method_errors_file);

        Set<String> filePaths = new HashSet<>();

        try {

            BufferedWriter ebw = new BufferedWriter(new FileWriter(method_errors_file, true));

            File f = new File(project_commits_path);
            if(f.isFile()) {
                String fn = f.getName();
                String project = fn.replace(".txt", "");

                String projectPath = resPath + File.separator + project;
                File fo = new File(projectPath);
                if (!fo.exists())
                    fo.mkdirs();
                Set<String> analyzedFRs = new HashSet<>();
                String project_resFile = resPath + File.separator + project + ".txt";
                if (new File(project_resFile).exists()) {
                    analyzedFRs = getAnalyzedFRs(project_resFile);
                    System.out.println("#Analyzed FRs: " + analyzedFRs.size());
                }

                String line;
                BufferedReader br = new BufferedReader(new FileReader(f));
                BufferedWriter bw = new BufferedWriter(new FileWriter(project_resFile, true));
                BufferedWriter bw1;
//                int num = 0;
                while((line = br.readLine())!=null) {
//                    if (num++ >= 100)
//                        break;
                    String[] sa = line.split(" ");
                    if (sa.length == 4) {
                        String commitId = sa[0];
//                        if(commitId.equals("0be4c31f80dc38ddf8decbc8d6d13bd23d3ae8b1")){
//                            System.out.println("stop");
//                        }
//                        else{continue;}
                        String oldPath = sa[2];
                        String FR = commitId + " " + oldPath;
                        if (analyzedFRs.contains(FR) || erroredFRs.contains(FR))
                            continue;

                        try {
                            filePaths.clear();
                            filePaths.add(oldPath);
                            CommitAnalysis mappingResult = new CommitAnalysis(project, commitId, filePaths, false);
                            long time1 = System.currentTimeMillis();
                            mappingResult.calResultMappings(false, false);
                            long time2 = System.currentTimeMillis();
                            long time = time2 - time1;

                            bw1 = new BufferedWriter(new FileWriter(
                                    projectPath + File.separator + commitId + ".txt",true));
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

                            for (String filePath: resultMap.keySet()){
                                RevisionAnalysis m = resultMap.get(filePath);
                                MappingStore ms = m.getMatcher().getMs();
                                List<StmtTokenAction> actionList = m.generateActions();
                                List<TreeEditAction> treeEditActions = m.generateEditActions();

                                bw1.write("\n\nFile: " + filePath + "\n\n");
                                bw1.write("************* Code Edit Script ***************\n");
                                for (StmtTokenAction action: actionList)
                                    bw1.write(action.toString());

                                bw1.write("\n\n************* AST Edit Script ***************\n");
                                for (TreeEditAction action: treeEditActions)
                                    bw1.write(action.toString());

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
                                bw.flush();
                            }
                            bw1.close();
                        } catch (Exception e) {
                            ebw.write(commitId + " " + oldPath + " -> " + e.getMessage() + "\n");
                            ebw.flush();
                        }
                    }
                }
                br.close();
                bw.close();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static Map<String, Integer> ruleFreqMap = new HashMap<>();


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

        String project_commits_path = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\project_commits-1\\activemq.txt";
//        Long datetime = System.currentTimeMillis();
//        Timestamp timestamp = new Timestamp(datetime);
//        String tmp = timestamp.toString().replace(" ", "|");
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedTimestamp = time.format(pattern);
//        System.out.println(formattedTimestamp);
        String iASTMapper_resPath = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\iASTMapper_res" + formattedTimestamp;
        String iASTMapper_errorsFile = iASTMapper_resPath + File.separator + "__errors_.txt";
        GitProjectInfoExtractor.createPath(iASTMapper_resPath);
        iASTMapper.used_ASTtype = "gt";
        run(project_commits_path, iASTMapper_errorsFile, iASTMapper_resPath);  // run iASTMapper

//        String iASTMapper_ruleFreqFile = "ase2023" + File.separator + "iASTMapper_rules.txt";
//        run4RuleFreqCal(project_commits_path, iASTMapper_ruleFreqFile);

//        String iASTMapper_IJMAST_resPath = "C:\\Users\\DELL\\Desktop\\iASTMapper\\ase2023\\iASTMapper(IJM-AST)_res";
//        String iASTMapper_IJMAST_errorsFile = iASTMapper_IJMAST_resPath + "\\__errors_.txt";
//        GitProjectInfoExtractor.createPath(iASTMapper_IJMAST_resPath);
//        iASTMapper.used_ASTtype = "ijm";
//        run(project_commits_path, iASTMapper_IJMAST_errorsFile, iASTMapper_IJMAST_resPath);  // run iASTMapper(IJM-AST)

    }

}
