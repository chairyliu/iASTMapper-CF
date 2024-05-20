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
    public static int crossFileCommitIdNum = 0;
    public static int crossFileStmtNum = 0;
    public static int multiFiles = 0;
    public static int totalCommitIdNum = 0;

    public static void run(String project_commits_path, String method_errors_file, String resPath){

        Set<String> erroredFRs = new HashSet<>();
        if (new File(method_errors_file).exists())
            erroredFRs = iASTMapper100_runner.getErroredFRs(method_errors_file);

        Set<String> filePaths = new HashSet<>();
        String previousCommitId = null;
        String recordedcommitId = null;

        try {

            BufferedWriter ebw = new BufferedWriter(new FileWriter(method_errors_file, true));

            File f = new File(project_commits_path);
            if(f.isFile()) {
                String fn = f.getName();
                String project = fn.replace(".txt", "");

                String projectPath = resPath + File.separator + project;//project就是activemq
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
                BufferedWriter bw = new BufferedWriter(new FileWriter(project_resFile, true));//activemq.txt这样的文件
                BufferedWriter bw1;//带commitId.txt的文件
                BufferedWriter bw2;//新增跨文件的输出（暂时）
                int num = 0;
                while((line = br.readLine())!=null) {
                    if (num++ >= 1000)
                        break;
                    String[] sa = line.split(" ");
                    if (sa.length == 4) {
                        String commitId = sa[0];
                        String oldPath = sa[2];
                        String FR = commitId + " " + oldPath;
                        if (analyzedFRs.contains(FR) || erroredFRs.contains(FR))
                            continue;

                        if (previousCommitId != null){
                            if (previousCommitId.equals(commitId))
                                continue;
                        }
                        previousCommitId = commitId;

                        try {
                            filePaths.clear();
                            filePaths.add(oldPath);
                            CommitAnalysis mappingResult = new CommitAnalysis(project, commitId, false);
                            long time1 = System.currentTimeMillis();
                            mappingResult.calResultMapping(false, false);
                            long time2 = System.currentTimeMillis();
                            long time = time2 - time1;
                            totalCommitIdNum++;

                            bw1 = new BufferedWriter(new FileWriter(
                                    projectPath + File.separator + commitId + ".txt",true));
                            bw2 = new BufferedWriter(new FileWriter(
                                    resPath + File.separator + project + " " + "cross-file output" + ".txt", true));
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

                            if (resultMap.size() != 1)
                                multiFiles++;

                            for (String filePath: resultMap.keySet()){
                                RevisionAnalysis m = resultMap.get(filePath);
                                MappingStore ms = m.getMatcher().getMs();
                                List<StmtTokenAction> actionList = m.generateActions(filePath);//如果这个只包含对应文件的action，那么可以再单独创建一个跨文件的输出
                                List<TreeEditAction> treeEditActions = m.generateEditActions();

                                bw1.write("\n\nFile: " + filePath + "\n\n");
                                bw1.write("************* Code Edit Script ***************\n");
                                for (StmtTokenAction action: actionList)
                                    bw1.write(action.toString());

                                bw1.write("\n\n************* AST Edit Script ***************\n");
                                for (TreeEditAction action: treeEditActions)
                                    bw1.write(action.toString());

                                //新增
                                iASTMapper matcher = m.getMatcher();
                                Map<Map<String, String>, String> crossFileMap = matcher.getCrossFileMap();
                                if (crossFileMap.size() != 0){
//                                    System.out.println(crossFileMap);
                                    if (recordedcommitId != null){
                                        if (!recordedcommitId.equals(commitId))
                                            crossFileCommitIdNum++;
                                    }
                                    recordedcommitId = commitId;
                                    bw2.write("\ncommitId: " + commitId + "\n");
                                    for (Map<String, String> pathToSrcStmt : crossFileMap.keySet()){
                                        String dstStmt = crossFileMap.get(pathToSrcStmt);
                                        crossFileStmtNum++;
                                        for (String path : pathToSrcStmt.keySet()){
                                            String srcStmt = pathToSrcStmt.get(path);
                                            String[] parts = path.split("\\+");
                                            bw2.write("srcFile: " + parts[0] + "\n");
                                            bw2.write("dstFile: " + parts[1] + "\n");
                                            bw2.write(srcStmt + " —> " + dstStmt + "\n\n");
                                        }
                                    }
                                }

                                int ASTNodeMappings_num = ms.size();//AST 节点之间的映射数量
                                int eleMappings_num = m.getMatcher().getEleMappings().asSet().size();//元素之间的映射数量
                                int ASTESSize = treeEditActions.size();//AST编辑操作数量
                                int CodeESSize = actionList.size();//code编辑操作数量
                                String record = commitId + " " + filePath + " -> " +
                                        ASTNodeMappings_num + " " + eleMappings_num + " " + ASTESSize +
                                        " " + CodeESSize + " " + time;
                                System.out.println(record);//输出框输出的内容
                                bw.write(record + "\n");
                                bw1.flush();
                                bw2.flush();
                                bw.flush();
                            }
                            bw1.close();
                            bw2.close();
                        } catch (Exception e) {
                            ebw.write(commitId + " " + oldPath + " -> " + e.getMessage() + "\n");
//                            ebw.write(commitId + " " + " -> " + e.getMessage() + "\n");
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

        System.out.println("The total commitId number is: " + totalCommitIdNum);
        System.out.println("multiple-files commitId number: " + multiFiles);
        System.out.println("cross-file commitId number: " + crossFileCommitIdNum);
        System.out.println("cross-file statements number: " + crossFileStmtNum);
//        System.out.println("The rate of cross-file mapping: " + (100 * crossFileCommitIdNum) / multiFiles + "%");

//        String iASTMapper_ruleFreqFile = "ase2023" + File.separator + "iASTMapper_rules.txt";
//        run4RuleFreqCal(project_commits_path, iASTMapper_ruleFreqFile);

//        String iASTMapper_IJMAST_resPath = "C:\\Users\\DELL\\Desktop\\iASTMapper\\ase2023\\iASTMapper(IJM-AST)_res";
//        String iASTMapper_IJMAST_errorsFile = iASTMapper_IJMAST_resPath + "\\__errors_.txt";
//        GitProjectInfoExtractor.createPath(iASTMapper_IJMAST_resPath);
//        iASTMapper.used_ASTtype = "ijm";
//        run(project_commits_path, iASTMapper_IJMAST_errorsFile, iASTMapper_IJMAST_resPath);  // run iASTMapper(IJM-AST)

    }

}
