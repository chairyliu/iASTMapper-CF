package ase2023;

import com.github.gumtreediff.matchers.MappingStore;
import cs.model.algorithm.actions.StmtTokenAction;
import cs.model.algorithm.actions.TreeEditAction;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.iASTMapper;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.rules.ElementMatchDeterminer;
import cs.model.algorithm.matcher.rules.ElementMatchRule;
import cs.model.algorithm.matcher.rules.MatchRulesConfiguration;
import cs.model.analysis.CommitAnalysis;
import cs.model.analysis.RevisionAnalysis;

import java.io.*;
import java.util.*;


public class iASTMapper_runner {

    public static int crossFileCommitIdNum = 0;
    public static int crossFileStmtNum = 0;
    public static int multiFiles = 0;
    public static int totalCommitIdNum = 0;
    public static void run(String project_commits_path, String method_errors_file, String resPath){

        Set<String> erroredFRs = new HashSet<>();
        if (new File(method_errors_file).exists())
            erroredFRs = iASTMapper_runner.getErroredFRs(method_errors_file);

        Set<String> filePaths = new HashSet<>();
        String previousCommitId = null;
        String recordedcommitId = null;

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
                        analyzedFRs = getAnalyzedFRs(project_resFile);
                        System.out.println("#Analyzed FRs: " + analyzedFRs.size());
                    }

                    String line;
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(project_resFile, true));
                    BufferedWriter bw1;
                    BufferedWriter bw2;//新增跨文件的输出（暂时）
                    while((line = br.readLine())!=null) {
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
                                        projectPath + "\\" + commitId + ".txt",true));
                                bw2 = new BufferedWriter(new FileWriter(
                                        resPath + "\\" + project + " " + "cross-file output" + ".txt", true));
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
                                    List<StmtTokenAction> actionList = m.generateActions(filePath);
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
                                    bw.flush();
                                }
                                bw1.close();
                                bw2.close();
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

    public static Map<String, Integer> ruleFreqMap = new HashMap<>();


    public static void run4RuleFreqCal(String project_commits_path, String method_rulefreq_file){

        ruleFreqMap.clear();
        Set<String> filePaths = new HashSet<>();

        try {

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

                    String line;
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    while((line = br.readLine())!=null) {
                        String[] sa = line.split(" ");
                        if (sa.length == 4) {
                            String commitId = sa[0];
                            String oldPath = sa[2];
                            // for test
//                            if (!commitId.equals("f3ef1a9a31c14e24c2c4587095be7191228af041") &&
//                                    !commitId.equals("262a5596d9300b7aded14d550cf8f5ee80d7ac0f"))
//                                continue;

                            try {
                                filePaths.clear();
                                filePaths.add(oldPath);
                                CommitAnalysis mappingResult = new CommitAnalysis(project, commitId, false);
                                mappingResult.calResultMapping(false, false);
                                for (String ruleName : iASTMapper.used_rules) {
                                    if (!ruleFreqMap.containsKey(ruleName))
                                        ruleFreqMap.put(ruleName, 0);
                                    ruleFreqMap.put(ruleName, ruleFreqMap.get(ruleName) + 1);
                                }
//                                Map<String, RevisionAnalysis> resultMap = mappingResult.getRevisionAnalysisResultMap();
//                                for (String filePath: resultMap.keySet()){
//                                    RevisionAnalysis m = resultMap.get(filePath);
//                                    calRuleFreqFromEleMappings(m.getMatcher().getEleMappings());
//                                }
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    br.close();
                }
            }

            Map<String, String> ruleNameMap = new HashMap<>();
//            ruleNameMap.put("SAME_STMT", "IDEN");
//            ruleNameMap.put("E_ANCESTOR", "ANCE");
//            ruleNameMap.put("DICE", "IMTR");
//            ruleNameMap.put("DM", "IMSR");
//            ruleNameMap.put("EXC", "S-ABS");
//            ruleNameMap.put("INNER_STMT_SAME_STMT", "I-MSIS");
//            ruleNameMap.put("INNER_STMT_ELE_DICE", "I-IMTR");
//            ruleNameMap.put("INNER_STMT_ELE_SANDWICH", "I-ABS");
//            ruleNameMap.put("STMT", "T-MSIS");
//            ruleNameMap.put("INNERSTMT", "T-MSIS");
//            ruleNameMap.put("TOKEN_LRB", "T-ABS");
            ruleNameMap.put("IDEN", "IDEN");
            ruleNameMap.put("ANCE", "ANCE");
            ruleNameMap.put("IMTR", "IMTR");
            ruleNameMap.put("IMSR", "IMSR");
            ruleNameMap.put("S-ABS", "S-ABS");
            ruleNameMap.put("I-MSIS", "I-MSIS");
            ruleNameMap.put("I-IMTR", "I-IMTR");
            ruleNameMap.put("I-ABS", "I-ABS");
            ruleNameMap.put("T-MSIS", "T-MSIS");
//            ruleNameMap.put("INNERSTMT", "T-MSIS");
            ruleNameMap.put("T-ABS", "T-ABS");

            int c = 0;
            BufferedWriter bw = new BufferedWriter(new FileWriter(method_rulefreq_file));
            for (String ruleName : ruleFreqMap.keySet()) {
                String aseRuleName = ruleName;
                if (ruleNameMap.containsKey(ruleName))
                    aseRuleName = ruleNameMap.get(ruleName);
                System.out.println(aseRuleName + " -> " + ruleFreqMap.get(ruleName));
                bw.write(aseRuleName + " " + ruleFreqMap.get(ruleName) + "\n");
                c += ruleFreqMap.get(ruleName);
            }
            bw.close();
            System.out.println(c);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Calculate the frequencies of the application of rules.
     */
    static void calRuleFreqFromEleMappings(ElementMappings eleMappings) {

        Map<ProgramElement, ProgramElement> srcToDst = eleMappings.getSrcToDst();
        if (srcToDst != null && srcToDst.size() > 0) {
            ElementMatchDeterminer determiner = new ElementMatchDeterminer(eleMappings);

            for (ProgramElement srcEle : srcToDst.keySet()) {
                if (srcEle.isStmt() || srcEle.isInnerStmtElement() || srcEle.isToken()) {
                    ProgramElement dstEle  = srcToDst.get(srcEle);
                    ElementSimMeasures elementSimMeasures = new ElementSimMeasures(srcEle, dstEle);
//                    SimMeasureConfiguration.getSimilarityMeasureConfiguration(srcEle);
                    if (!srcEle.isStmt())
                        continue;

                    for (String ruleName : MatchRulesConfiguration.getRuleConfiguration(srcEle)) {
                        System.out.println("ruleName: " + ruleName);
                        ElementMatchRule rule = determiner.getElementMatchRule(ruleName);
                        if (rule.determineCanBeMapped(elementSimMeasures, eleMappings)) {
                            if (!ruleFreqMap.containsKey(ruleName))
                                ruleFreqMap.put(ruleName, 0);
                            ruleFreqMap.put(ruleName, ruleFreqMap.get(ruleName) + 1);
//                            break;
                        }
                    }
                }
            }
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

        String project_commits_path = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\project_commits-1";

        String iASTMapper_resPath = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\iASTMapper_res";
        String iASTMapper_errorsFile = iASTMapper_resPath + "\\__errors_.txt";
        GitProjectInfoExtractor.createPath(iASTMapper_resPath);
        iASTMapper.used_ASTtype = "gt";
//        System.out.println(666);
        run(project_commits_path, iASTMapper_errorsFile, iASTMapper_resPath);  // run iASTMapper

//        System.out.println(6666);
        String iASTMapper_ruleFreqFile = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\iASTMapper_rules.txt";
        run4RuleFreqCal(project_commits_path, iASTMapper_ruleFreqFile);

        System.out.println("The total commitId number is: " + totalCommitIdNum);
        System.out.println("multiple-files commitId number: " + multiFiles);
        System.out.println("cross-file commitId number: " + crossFileCommitIdNum);
        System.out.println("cross-file statements number: " + crossFileStmtNum);
//        String iASTMapper_IJMAST_resPath = "C:\\Users\\DELL\\Desktop\\iASTMapper\\ase2023\\iASTMapper(IJM-AST)_res";
//        String iASTMapper_IJMAST_errorsFile = iASTMapper_IJMAST_resPath + "\\__errors_.txt";
//        GitProjectInfoExtractor.createPath(iASTMapper_IJMAST_resPath);
//        iASTMapper.used_ASTtype = "ijm";
//        run(project_commits_path, iASTMapper_IJMAST_errorsFile, iASTMapper_IJMAST_resPath);  // run iASTMapper(IJM-AST)

    }

}
