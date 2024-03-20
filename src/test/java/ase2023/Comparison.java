package ase2023;

import cs.model.utils.CosSimilarity;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Comparison {

    public static int count = 0;
    private static Set<String> processedCommitIds = new HashSet<>();
    public static int blocksCountA = 0;
    public static int blocksCountB = 0;
    public static int tokensCountA = 0;
    public static int tokensCountB = 0;
    public static int totalBlocksA = 0;
    public static int totalBlocksB = 0;
    public static int totalTokensA = 0;
    public static int totalTokensB = 0;
    public static int totalChangedTokens = 0;
    public static int changedTokensNumber = 0;
    public static List<Integer> unMappedList = new ArrayList<>();
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

    public static TreeMap<Integer, List<String>> split2Block(List<String> script){//按照标记分块，每个块含有分隔符之间的所有行
        TreeMap<Integer, List<String>> blocks = new TreeMap<>();
        int blockIndex = 0; // 用于给块编号
        List<String> currentBlock = new ArrayList<>();

        for (String line : script) {
            if (line.startsWith("==================================================")) {
                if (!currentBlock.isEmpty()) {
                    // 将当前块的内容存储到blocks中，使用blockIndex作为键
                    blocks.put(blockIndex, currentBlock);
                    currentBlock = new ArrayList<>(); // 开始新块的内容
                    blockIndex++; // 块编号增加
                }
            } else {
                currentBlock.add(line);
            }
        }
        // 检查是否有最后一个块需要添加
        if (!currentBlock.isEmpty()) {
            blocks.put(blockIndex, currentBlock);
        }
        return blocks;
    }

    public static void compare(String projectPathA, String projectPathB, List<String> commits, List<String> outputLines){
        // for each commits
        for (String commitId : commits) {
            compareEle(projectPathA, projectPathB, commitId, outputLines);
        }
    }

    public static void compareEle(String projectPathA, String projectPathB, String commitId, List<String> outputLines) {
        // Check if the commitId has already been processed
        if (processedCommitIds.contains(commitId)) {
            // Skip this commitId as it has already been processed
            return;
        }
        // Add commitId to the set to mark it as processed
        processedCommitIds.add(commitId);
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
//        List<FileScripts> fileScriptsListA = splitCommitFile(fA);
//        List<FileScripts> fileScriptsListB = splitCommitFile(fB);
        /** Iterate the CodeEditScript over mapA and mapB at the same time,
         * divide them into multiple blocks according to ==================================================,
         * and output different blocks
         **/
        List<String> CodeEditScriptA = mapA.get("CodeEditScript");
        List<String> CodeEditScriptB = mapB.get("CodeEditScript");
//        List<String> ASTEditScriptA = mapA.get("ASTEditScript");
//        List<String> ASTEditScriptB = mapB.get("ASTEditScript");
        TreeMap<Integer, List<String>> blocksMapA = split2Block(CodeEditScriptA);
        TreeMap<Integer, List<String>> blocksMapB = split2Block(CodeEditScriptB);
        blocksCountA += blocksMapA.size();
        blocksCountB += blocksMapB.size();
//        List<String> ASTEditScriptA_block = split2Block(ASTEditScriptA);
//        List<String> ASTEditScriptB_block = split2Block(ASTEditScriptB);
        List<Integer> Diff_CodeEditScriptBlockIndex = new ArrayList<>();
        List<String> Diff_CodeEditScriptList = new ArrayList<>();
//        List<Integer> Diff_ASTEditScriptBlockIndex = new ArrayList<>();
        List<String> CodeEditScriptA_block = new ArrayList<>();
        List<String> CodeEditScriptB_block = new ArrayList<>();

        /**
         * compare CodeEditScriptA_block and CodeEditScriptB_block, adding the diff into Diff_CodeEditScriptBlockIndex
         **/
        List<Integer> keysA = new ArrayList<>(blocksMapA.keySet());
        Iterator<Integer> iteratorA = keysA.iterator();
        while (iteratorA.hasNext()) {
            Integer blockIndex = iteratorA.next();
//        for (Integer blockIndex : blocksMapA.keySet()) {
            if (blocksMapB.containsKey(blockIndex) && blocksMapA.containsKey(blockIndex)) {
                CodeEditScriptA_block = blocksMapA.get(blockIndex);
                CodeEditScriptB_block = blocksMapB.get(blockIndex);
                tokensCountA += CodeEditScriptA_block.size();
                tokensCountB += CodeEditScriptB_block.size();
                int nextBlockIndex = blockIndex + 1;
                int i = 0, j = 0;
                while (i < CodeEditScriptA_block.size() && j < CodeEditScriptB_block.size()) {
                    if (CodeEditScriptA_block.get(i).equals(CodeEditScriptB_block.get(j))) {
                        i++;
                        j++;
                    } else {
                        // 行不匹配，尝试查找错位匹配
                        int nextA = findNextMatch(CodeEditScriptA_block, CodeEditScriptB_block.get(j), i + 1);
                        int nextB = findNextMatch(CodeEditScriptB_block, CodeEditScriptA_block.get(i), j + 1);

                        if (nextA != -1 && (nextA < nextB || nextB == -1)) {
                            // A中找到更接近的匹配或B中没有找到匹配
                            for (int l = i; l < nextA; l++) {
                                Diff_CodeEditScriptBlockIndex.add(l);
                                String m = CodeEditScriptA_block.get(l);
                                Diff_CodeEditScriptList.add(m);
                            }
                            i = nextA + 1;
                            j++;
                        } else if (nextB != -1) {
                            for (int l = j; l < nextB; l++) {
                                Diff_CodeEditScriptBlockIndex.add(l);
                                String n = CodeEditScriptB_block.get(l);
                                Diff_CodeEditScriptList.add(n);
                                changedTokensNumber++;
                            }
                            j = nextB + 1;
                            i++;
                        } else if (isOffSetBlockMatching(blocksMapA, nextBlockIndex, CodeEditScriptB_block)) {
                            for (int p = nextBlockIndex; p < blocksMapA.size(); p++) {
                                if (blocksMapA.containsKey(p) && CodeEditScriptB_block.get(0).equals(blocksMapA.get(p).get(0))) {
                                    for (int b = blockIndex; b < p; b++) {
                                        List<String> offSet_CodeEditScriptA = blocksMapA.get(b);
                                        if (!isOffSetBlockMatching(blocksMapB, 0, blocksMapA.get(b))) {
                                            for (int k = 0; k < blocksMapA.get(b).size(); k++) {
                                                Diff_CodeEditScriptBlockIndex.add(k);
                                                String m = blocksMapA.get(b).get(k);
                                                Diff_CodeEditScriptList.add(m);
                                            }
                                            unMappedList.add(b);
//                                            System.out.println("2");
                                        }
                                    }
                                    if (!unMappedList.isEmpty()) {
                                        if (unMappedList.size() == 1) {
                                            removeBlockAndUpdateIndices(blocksMapA, unMappedList.get(0));
                                            iteratorA.remove();
                                        } else if (unMappedList.get(0) == blockIndex) {
                                            removeBlockAndUpdateIndices(blocksMapA, blockIndex);
                                            iteratorA.remove();
                                            unMappedList.remove(blockIndex);
                                            Collections.sort(unMappedList, Collections.reverseOrder());
                                            while (iteratorA.hasNext()) {
                                                Integer currentBlockIndex = iteratorA.next();
                                                int m = currentBlockIndex - 1;
                                                if (unMappedList.contains(m)) {
                                                    removeBlockAndUpdateIndices(blocksMapA, m);
                                                    iteratorA.remove();
                                                }
                                            }
                                        } else {
                                            Collections.sort(unMappedList, Collections.reverseOrder());
                                            int iteratorCount = 0;
                                            while (iteratorA.hasNext() && iteratorCount < unMappedList.size()) {
                                                Integer currentBlockIndex = iteratorA.next();
                                                if (unMappedList.contains(currentBlockIndex)) {
                                                    removeBlockAndUpdateIndices(blocksMapA, currentBlockIndex);
                                                    iteratorA.remove();
                                                    iteratorCount++;
                                                }
                                            }
                                        }
                                    }
//                                    System.out.println("0");
                                    break;
                                }
                            }
                            unMappedList.clear();
                            break;
                        } else if (isOffSetBlockMatching(blocksMapB, nextBlockIndex, CodeEditScriptA_block)) {
                            for (int q = nextBlockIndex; q < blocksMapB.size(); q++) {
                                if (blocksMapB.containsKey(q) && CodeEditScriptA_block.get(0).equals(blocksMapB.get(q).get(0))) {
                                    for (int a = blockIndex; a < q; a++) {
                                        List<String> offSet_CodeEditScriptB = blocksMapB.get(a);
                                        if (!isOffSetBlockMatching(blocksMapA, 0, blocksMapB.get(a))){
                                            for (int k = 0; k < blocksMapB.get(a).size(); k++) {
                                                Diff_CodeEditScriptBlockIndex.add(k);
                                                String n = blocksMapB.get(a).get(k);
                                                Diff_CodeEditScriptList.add(n);
                                                changedTokensNumber++;
                                            }
                                            unMappedList.add(a);
//                                            System.out.println("6");
                                        }
                                    }
                                    Collections.sort(unMappedList, Collections.reverseOrder());
                                    for (int unMappedIndex : unMappedList){
                                        removeBlockAndUpdateIndices(blocksMapB, unMappedIndex);
                                    }
//                                    System.out.println("8");
                                    break;
                                }
                            }
                            unMappedList.clear();
                            break;
                        } else {
                            // 两边都没有找到匹配，记录当前不匹配的行
                            String m = CodeEditScriptA_block.get(i);
                            String n = CodeEditScriptB_block.get(j);
                            if (compareTwo(m, n)){
                                Diff_CodeEditScriptBlockIndex.add(i);
                                Diff_CodeEditScriptList.add(m);
                                Diff_CodeEditScriptBlockIndex.add(j);
                                Diff_CodeEditScriptList.add(n);
                                changedTokensNumber++;
//                            System.out.println("4");
                            }
                            i++;
                            j++;
                        }
                    }
                }
            }else if (blocksMapA.containsKey(blockIndex)){
                // 如果 B 中没有对应的块，那么 A 中的这个块全是不匹配的
                List<String> unMappedBlockA = blocksMapA.get(blockIndex);
                tokensCountA += unMappedBlockA.size();
                for (int i = 0; i < unMappedBlockA.size(); i++) {
                    Diff_CodeEditScriptBlockIndex.add(i);
                    String m = blocksMapA.get(blockIndex).get(i);
                    Diff_CodeEditScriptList.add(m);
                }
            }
        }
        for (Integer blockIndex : blocksMapB.keySet()) {
            if (!blocksMapA.containsKey(blockIndex)) {
                List<String> unMappedBlockB = blocksMapB.get(blockIndex);
                if (!isOffSetBlockMatching(blocksMapA, 0, unMappedBlockB)){
                    tokensCountB += unMappedBlockB.size();
                    for (int j = 0; j < unMappedBlockB.size(); j++) {
                        Diff_CodeEditScriptBlockIndex.add(j);
                        String n = blocksMapB.get(blockIndex).get(j);
                        Diff_CodeEditScriptList.add(n);
                        changedTokensNumber++;
                    }
                }
            }
        }

        /**
         * compare ASTEditScriptA_block and ASTEditScriptB_block, adding the diff into Diff_ASTEditScriptBlockIndex
         **/
//        for (int j = 0; j < Math.max(ASTEditScriptA_block.size(), ASTEditScriptB_block.size()); j++) {
//            if (Math.min(ASTEditScriptA_block.size(), ASTEditScriptB_block.size()) < j + 1){
//                Diff_ASTEditScriptBlockIndex.add(j);
//            }
//            else{
//                String lineA = ASTEditScriptA_block.get(j);
//                String lineB = ASTEditScriptB_block.get(j);
//                if (!lineA.equals(lineB)) {
//                    Diff_ASTEditScriptBlockIndex.add(j);
//                }
//            }
//        }
        /**
         * output the diff blocks
         **/
//        if (Diff_CodeEditScriptBlockIndex.size() == 0 && Diff_ASTEditScriptBlockIndex.size() == 0) {
//            return;
//        }
        if (Diff_CodeEditScriptBlockIndex.size() == 0) {
            return;
        }
        /**
         * record update
         * count (%)
         */
        count++;
//        System.out.println("commitId: " + commitId);
//        System.out.println("CodeEditScriptBlockIndex: " + Diff_CodeEditScriptBlockIndex);
//        System.out.println("ASTEditScriptBlockIndex: " + Diff_ASTEditScriptBlockIndex);
//        System.out.println("CodeEditScriptBlock: ");
        totalBlocksA += blocksCountA;
        totalBlocksB += blocksCountB;
        totalTokensA += tokensCountA;
        totalTokensB += tokensCountB;
        totalChangedTokens += changedTokensNumber;
        outputLines.add("");
        outputLines.add("commitId: " + commitId);
        outputLines.add("CodeEditScriptBlockIndex: " + Diff_CodeEditScriptBlockIndex);
//        outputLines.add("ASTEditScriptBlockIndex: " + Diff_ASTEditScriptBlockIndex);
        outputLines.add("the number of statements before deleting a rule is: " + blocksCountA);
        outputLines.add("the number of statements after deleting a rule is: " + blocksCountB);
        outputLines.add("the initial tokens number is: " + tokensCountA);
        outputLines.add("the final tokens number is: " + tokensCountB);
        outputLines.add("the number of changed tokens is: " + changedTokensNumber);
        outputLines.add("CodeEditScriptBlock: ");
        for (int j = 0; j < Diff_CodeEditScriptList.size(); j++){
            outputLines.add(Diff_CodeEditScriptList.get(j));
        }
        blocksCountA = 0;
        blocksCountB = 0;
        tokensCountA = 0;
        tokensCountB = 0;
        changedTokensNumber = 0;
//        if (CodeEditScriptA_block.size() > CodeEditScriptB_block.size()) {
//            for (int j = 0; j < Diff_CodeEditScriptBlockIndex.size(); j++) {
//                int index = Diff_CodeEditScriptBlockIndex.get(j);
////                System.out.println(CodeEditScriptA_block.get(index));
//                outputLines.add(CodeEditScriptA_block.get(index));
//            }
//        }
//        else {
//            for (int j = 0; j < Diff_CodeEditScriptBlockIndex.size(); j++) {
//                int index = Diff_CodeEditScriptBlockIndex.get(j);
////                System.out.println(CodeEditScriptB_block.get(index));
//                outputLines.add(CodeEditScriptB_block.get(index));
//            }
//        }
//        System.out.println("ASTEditScriptBlock: ");
//        outputLines.add("ASTEditScriptBlock: ");
//        if (ASTEditScriptA_block.size() > ASTEditScriptB_block.size()) {
//            for (int j = 0; j < Diff_ASTEditScriptBlockIndex.size(); j++) {
//                int index = Diff_ASTEditScriptBlockIndex.get(j);
////                System.out.println(ASTEditScriptA_block.get(index));
//                outputLines.add(ASTEditScriptA_block.get(index));
//            }
//        }
//        else {
//            for (int j = 0; j < Diff_ASTEditScriptBlockIndex.size(); j++) {
//                int index = Diff_ASTEditScriptBlockIndex.get(j);
////                System.out.println(ASTEditScriptB_block.get(index));
//                outputLines.add(ASTEditScriptB_block.get(index));
//            }
//        }
    }

    // findNextMatch 方法用来查找从startIndex开始第一个匹配给定行的行索引
    public static int findNextMatch(List<String> block, String lineToMatch, int startIndex) {
        for (int k = startIndex; k < block.size(); k++) {
            if (block.get(k).equals(lineToMatch)) {
//                System.out.println("2");
                return k;
            }
        }
        return -1;
    }

    private static boolean isOffSetBlockMatching(Map<Integer, List<String>> blocksMap, int blockIndex, List<String> targetBlock) {
        for (int m = blockIndex; m < blocksMap.size(); m++){
            if (targetBlock.get(0).equals(blocksMap.get(m).get(0))){
                return true;
            }
        }
        return false;
    }

    private static boolean isMutilLinesMatching(Map<Integer, List<String>> blocksMap, int blockIndex, List<String> targetBlock) {
        for (int m = blockIndex; m < blocksMap.size(); m++) {
            int equalsLine = 0;
            for (int k = 0; k < Math.min(blocksMap.get(m).size(), targetBlock.size()); k++) {
                String lineA = blocksMap.get(m).get(k);
                String lineB = targetBlock.get(k);
                if (lineA.equals(lineB)) {
                    equalsLine++;
                }
//                System.out.println("1");
            }
            double rate = (double) equalsLine / Math.min(blocksMap.get(m).size(), targetBlock.size());
            if (targetBlock.get(0).equals(blocksMap.get(m).get(0)) || rate > 0.5){
//                System.out.println("0");
                return true;
            }
        }
        return false;
    }

    private static void removeBlockAndUpdateIndices(TreeMap<Integer, List<String>> blocksMap, Integer blockIndex) {
        blocksMap.remove(blockIndex);
        TreeMap<Integer, List<String>> newBlocksMap = new TreeMap<>();
        for (Map.Entry<Integer, List<String>> entry : blocksMap.entrySet()) {
            Integer key = entry.getKey();
            List<String> value = entry.getValue();
            if (key > blockIndex) {
                newBlocksMap.put(key - 1, value);
            } else {
                newBlocksMap.put(key, value);
            }
        }
        blocksMap.clear();
        blocksMap.putAll(newBlocksMap);
    }

    public static boolean compareTwo(String src, String dst) {
        String[] srcTokens = src.split("\\s+");
        String[] dstTokens = dst.split("\\s+");
        Map<CharSequence, Integer> srcMaptokens = convertToMap(srcTokens);
        Map<CharSequence, Integer> dstMaptokens = convertToMap(dstTokens);
        CosSimilarity simEngine = new CosSimilarity();
        double comparisonSimilarity = simEngine.cosineSimilarity(srcMaptokens, dstMaptokens);
        if (comparisonSimilarity > 0.3){
            return true;
        } else {
            return false;
        }
    }

    private static Map<CharSequence, Integer> convertToMap(String[] tokens) {
        Map<CharSequence, Integer> map = new HashMap<>();
        for (String token : tokens) {
            map.put(token, map.getOrDefault(token, 0) + 1);
        }
        return map;
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
        String projectPathA = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\iASTMapper_res_INNERSTMT\\activemq";
        String projectPathB = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\iASTMapper_res20240320203638\\activemq";
//        System.out.println(projectPath);
        // we only need to compare the first 100 commits,
        // and projectA and projectB should have the same commits lists
        List<String> commits = getFirst100Commits(projectPathA);
//        System.out.println(commits);
//        System.out.println(commits.size());
        // for each commit, we compare Code Edit Script and AST Edit Script
//        compare(projectPathA, projectPathB, commits);
        List<String> outputLines = new ArrayList<>();
        compare(projectPathA, projectPathB, commits, outputLines);
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedTimestamp = time.format(pattern);
        try (PrintWriter writer = new PrintWriter(new File("C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\output.txt" + formattedTimestamp), "UTF-8")) {
            for (String line : outputLines) {
                writer.println(line);
            }
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        System.out.println("Total difference: " + count +" [" + (double)count + "%]");
        System.out.println("Total difference: " + count +" [" + (double)(100*count/commits.size()) + "%]");
        System.out.println("the total statement of A is: " + totalBlocksA);
        System.out.println("the total statement of B is: " + totalBlocksB);
        System.out.println("the total tokens of A is: " + totalTokensA);
        System.out.println("the total tokens of B is: " + totalTokensB);
        System.out.println("the changed tokens number is: " + totalChangedTokens);
    }
}
