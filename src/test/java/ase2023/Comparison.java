package ase2023;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Comparison {

    public static int count = 0;

    /**
     * Get the first 100 commits from the file
     * @param projectPath
     * @return
     */
    public static List<String> getFirst100Commits(String projectPath) {
        File f = new File(projectPath + ".txt");
        if (!f.exists()) {
            throw new RuntimeException("File not found: " + f.getAbsolutePath());
        }
        List<String> commits = new ArrayList<>();
        try {
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(f));
            int i = 0;
            while((line = br.readLine())!=null) {
                if (i++ >= 1000)
                    break;
                String[] sa = line.split(" ");
                if (sa.length == 8) {
                    String commitId = sa[0];
                    commits.add(commitId);
                }
            }
            br.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return commits;
    }

    public static List<String> split2Block(List<String> script){//按照标记分块，每个块含有分隔符之间的所有行
        List<String> blocks = new ArrayList<>();;
        int i = 0;
        for (String lineB : script) {
            if (lineB.startsWith("==================================================")) {
                i++;
                continue;
            }
            blocks.add(lineB);
        }
        return blocks;
    }

    public static void compare(String projectPathA, String projectPathB, List<String> commits){
        // for each commits
        for (String commitId : commits) {
            compareEle(projectPathA, projectPathB, commitId);
        }
    }

    public static void compareEle(String projectPathA, String projectPathB, String commitId){
        String commitPathA = projectPathA + File.separator + commitId + ".txt";
        String commitPathB = projectPathB + File.separator + commitId + ".txt";
        File fA = new File(commitPathA);
        File fB = new File(commitPathB);
        if (!fA.exists() || !fB.exists()) {
            throw new RuntimeException("File not found: " + fA.getAbsolutePath() + " or " + fB.getAbsolutePath());
        }
        // split txt file by two parts, first is ************* Code Edit Script ***************, second is ************* AST Edit Script ***************
        Map<String, List<String>> mapA = splitCommitFile(fA);
        Map<String, List<String>> mapB = splitCommitFile(fB);
        /** Iterate the CodeEditScript over mapA and mapB at the same time,
         * divide them into multiple blocks according to ==================================================,
         * and output different blocks
        **/
        List<String> CodeEditScriptA = mapA.get("CodeEditScript");
        List<String> CodeEditScriptB = mapB.get("CodeEditScript");
        List<String> ASTEditScriptA = mapA.get("ASTEditScript");
        List<String> ASTEditScriptB = mapB.get("ASTEditScript");
        List<String> CodeEditScriptA_block = split2Block(CodeEditScriptA);
        List<String> CodeEditScriptB_block = split2Block(CodeEditScriptB);
        List<String> ASTEditScriptA_block = split2Block(ASTEditScriptA);
        List<String> ASTEditScriptB_block = split2Block(ASTEditScriptB);
        List<Integer> Diff_CodeEditScriptBlockIndex = new ArrayList<>();
        List<Integer> Diff_ASTEditScriptBlockIndex = new ArrayList<>();
        /**
         * compare CodeEditScriptA_block and CodeEditScriptB_block, adding the diff into Diff_CodeEditScriptBlockIndex
         **/
        for (int j = 0; j < Math.max(CodeEditScriptA_block.size(), CodeEditScriptB_block.size()); j++) {
            if (Math.min(CodeEditScriptA_block.size(), CodeEditScriptB_block.size()) < j + 1){
                Diff_CodeEditScriptBlockIndex.add(j);
            }
            else {
                String lineA = CodeEditScriptA_block.get(j);
                String lineB = CodeEditScriptB_block.get(j);
                if (!lineA.equals(lineB)) {
                    Diff_CodeEditScriptBlockIndex.add(j);
                }
            }
        }
        /**
         * compare ASTEditScriptA_block and ASTEditScriptB_block, adding the diff into Diff_ASTEditScriptBlockIndex
         **/
        for (int j = 0; j < Math.max(ASTEditScriptA_block.size(), ASTEditScriptB_block.size()); j++) {
            if (Math.min(ASTEditScriptA_block.size(), ASTEditScriptB_block.size()) < j + 1){
                Diff_ASTEditScriptBlockIndex.add(j);
            }
            else{
                String lineA = ASTEditScriptA_block.get(j);
                String lineB = ASTEditScriptB_block.get(j);
                if (!lineA.equals(lineB)) {
                    Diff_ASTEditScriptBlockIndex.add(j);
                }
            }
        }
        /**
         * output the diff blocks
         **/
        if (Diff_CodeEditScriptBlockIndex.size() == 0 && Diff_ASTEditScriptBlockIndex.size() == 0) {
            return;
        }
        /**
         * record update
         * count (%)
         */
        count++;
        System.out.println("commitId: " + commitId);
        System.out.println("CodeEditScriptBlockIndex: " + Diff_CodeEditScriptBlockIndex);
        System.out.println("ASTEditScriptBlockIndex: " + Diff_ASTEditScriptBlockIndex);
        System.out.println("CodeEditScriptBlock: ");
        if (CodeEditScriptA_block.size() > CodeEditScriptB_block.size()) {
            for (int j = 0; j < Diff_CodeEditScriptBlockIndex.size(); j++) {
                int index = Diff_CodeEditScriptBlockIndex.get(j);
                System.out.println(CodeEditScriptA_block.get(index));
            }
        }
        else {
            for (int j = 0; j < Diff_CodeEditScriptBlockIndex.size(); j++) {
                int index = Diff_CodeEditScriptBlockIndex.get(j);
                System.out.println(CodeEditScriptB_block.get(index));
            }
        }
        System.out.println("ASTEditScriptBlock: ");
        if (ASTEditScriptA_block.size() > ASTEditScriptB_block.size()) {
            for (int j = 0; j < Diff_ASTEditScriptBlockIndex.size(); j++) {
                int index = Diff_ASTEditScriptBlockIndex.get(j);
                System.out.println(ASTEditScriptA_block.get(index));
            }
        }
        else {
            for (int j = 0; j < Diff_ASTEditScriptBlockIndex.size(); j++) {
                int index = Diff_ASTEditScriptBlockIndex.get(j);
                System.out.println(ASTEditScriptB_block.get(index));
            }
        }
    }

    public static Map<String, List<String>> splitCommitFile(File f){
        Map<String, List<String>> map = new HashMap<>();
        List<String> CodeEditScript = new ArrayList<>();
        List<String> ASTEditScript = new ArrayList<>();
        /**
         * // split txt file by two parts,
         * first is after ************* Code Edit Script ***************,
         * second is after ************* AST Edit Script ***************
         */
        try {
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(f));
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("************* Code Edit Script ***************")) {
                    i = 1;
                    continue;
                }
                if (line.startsWith("************* AST Edit Script ***************")) {
                    i = 2;
                    continue;
                }
                if (i == 1) {
                    CodeEditScript.add(line);
                }
                if (i == 2) {
                    ASTEditScript.add(line);
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        map.put("CodeEditScript", CodeEditScript);
        map.put("ASTEditScript", ASTEditScript);
        return map;
    }

    public static void main(String[] args) {
        // project name
        String projectPathA = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\iASTMapper_res_P0_1000\\activemq";
        String projectPathB = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\iASTMapper_res20240224101216\\activemq";
//        System.out.println(projectPath);
        // we only need to compare the first 100 commits,
        // and projectA and projectB should have the same commits lists
        List<String> commits = getFirst100Commits(projectPathA);
//        System.out.println(commits);
//        System.out.println(commits.size());
        // for each commit, we compare Code Edit Script and AST Edit Script
        compare(projectPathA, projectPathB, commits);
        System.out.println("Total difference: " + count +" [" + (double)count + "%]");
//        System.out.println("Total difference: " + count +" [" + (double)(100*count/commits.size()) + "%]");
    }
}
