package ase2023;

import cs.model.algorithm.actions.StmtTokenAction;
import cs.model.algorithm.actions.StmtTokenActionGenerator;
import cs.model.algorithm.actions.TreeEditAction;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.baseline.BaselineMatcher;
import cs.model.utils.Pair;
import utils.TestUtils;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Baseline_runner {

    public static void run(String project_commits_path, String method, String method_errors_file, String resPath){

        Set<String> erroredFRs = new HashSet<>();
        if (new File(method_errors_file).exists())
            erroredFRs = iASTMapper_runner.getErroredFRs(method_errors_file);

        try {

            BufferedWriter ebw = new BufferedWriter(new FileWriter(method_errors_file, true));

            File pfo = new File(project_commits_path);
            for (File f : pfo.listFiles()) {
                if(f.isFile()) {
                    String fn = f.getName();
                    if (!fn.endsWith(".txt"))
                        continue;
                    String project = fn.replace(".txt", "");
                    // for test
    //                    if (!project.equals("activemq"))
    //                        continue;
                    String projectPath = resPath + "\\" + project;
                    File fo = new File(projectPath);
                    if (!fo.exists())
                        fo.mkdirs();

                    Set<String> analyzedFRs = new HashSet<>();
                    String project_resFile = resPath + "\\" + project + ".txt";
                    if (new File(project_resFile).exists()) {
                        analyzedFRs = iASTMapper_runner.getAnalyzedFRs(project_resFile);
                        System.out.println("#Analyzed FRs: " + analyzedFRs.size());
                    }

                    String line;
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(project_resFile, true));
                    BufferedWriter bw1;
                    while((line = br.readLine())!=null) {
                        String[] sa = line.split(" ");
                        if (sa.length == 4) {
                            String commitId = sa[0];
                            String oldPath = sa[2];
                            String FR = commitId + " " + oldPath;
                            if (analyzedFRs.contains(FR) || erroredFRs.contains(FR))
                                continue;
                            // for test
    //                            if (!commitId.equals("f3ef1a9a31c14e24c2c4587095be7191228af041") &&
    //                                    !commitId.equals("262a5596d9300b7aded14d550cf8f5ee80d7ac0f"))
    //                                continue;
                            try{

                                long time1 = System.currentTimeMillis();
                                Pair<String, String> contentPair =
                                        TestUtils.getSrcAndDstFileContent(project, commitId, oldPath);
                                if (contentPair == null)
                                    continue;
                                String srcFileContent = contentPair.first;
                                String dstFileContent = contentPair.second;
                                BaselineMatcher matcher = new BaselineMatcher(srcFileContent, dstFileContent);

                                ElementMappings eleMappings = matcher.getOriginalMethodMapping(method);
                                long time2 = System.currentTimeMillis();
                                long time = time2 - time1;

                                StmtTokenActionGenerator generator = new StmtTokenActionGenerator(
                                        matcher.getSrcStmtElements(), matcher.getDstStmtElements(), eleMappings);
                                List<StmtTokenAction> actionList = generator.generateActions(false);
                                List<TreeEditAction> treeEditActions = matcher.getTreeEditActions();
                                bw1 = new BufferedWriter(new FileWriter(
                                        projectPath + "\\" + commitId + ".txt", true));

                                bw1.write("\n\nFile: " + oldPath + "\n\n");
                                bw1.write("************* Code Edit Script ***************\n");
                                for (StmtTokenAction action: actionList)
                                    bw1.write(action.toString());

                                bw1.write("\n\n************* AST Edit Script ***************\n");
                                for (TreeEditAction action: treeEditActions)
                                    bw1.write(action.toString());

                                int ASTNodeMappings_num = matcher.getMs().size();
                                int eleMappings_num = eleMappings.asSet().size();
                                int ASTESSize = treeEditActions.size();
                                int CodeESSize = actionList.size();
                                String record = commitId + " " + oldPath + " -> " +
                                        ASTNodeMappings_num + " " + eleMappings_num + " " + ASTESSize + " " + CodeESSize + " " + time;
                                System.out.println(record);
                                bw.write(record + "\n");
                                bw1.close();
                                bw.flush();
                            } catch (Exception e) {
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
     *
     * @param args
     */
    public static void main(String[] args) {

        String project_commits_path = "C:\\Users\\HP\\Desktop\\iASTMapper\\ase2023\\project_commits-1";

        String GT_resPath = "C:\\Users\\HP\\Desktop\\iASTMapper\\ase2023\\GT_res";
        String GT_errorsFile = GT_resPath + "\\__errors_.txt";
        GitProjectInfoExtractor.createPath(GT_resPath);
        run(project_commits_path, "gt", GT_errorsFile, GT_resPath);

        String IJM_resPath = "C:\\Users\\HP\\Desktop\\iASTMapper\\ase2023\\IJM_res";
        String IJM_errorsFile = IJM_resPath + "\\__errors_.txt";
        GitProjectInfoExtractor.createPath(IJM_resPath);
        run(project_commits_path, "ijm", IJM_errorsFile, IJM_resPath);

        String MTD_resPath = "C:\\Users\\HP\\Desktop\\iASTMapper\\ase2023\\MTD_res";
        String MTD_errorsFile = MTD_resPath + "\\__errors_.txt";
        GitProjectInfoExtractor.createPath(MTD_resPath);
        run(project_commits_path, "mtd", MTD_errorsFile, MTD_resPath);

    }

}
