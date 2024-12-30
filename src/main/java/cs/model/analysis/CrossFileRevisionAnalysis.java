package cs.model.analysis;

import com.github.gumtreediff.tree.ITree;
import cs.model.algorithm.utils.DeclarationUtil;
import cs.model.algorithm.utils.GumTreeUtil;
import cs.model.gitops.GitInfoRetrieval;
import cs.model.gitops.GitUtils;
import cs.model.utils.CosSimilarity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.util.*;


public class CrossFileRevisionAnalysis {
    protected String srcFileContent;
    protected String dstFileContent;
    public static String used_ASTType = "gt";
    public Integer methodDeclarationNum;
    public List<String> crossFileMappingList = new ArrayList<>();
    public Integer crossTransferMethodDeclarationNum;
    public Map<String, List<String>> srcPathToMethodsMap;
    public Map<String, List<String>> dstPathToMethodsMap;
    public List<String> allDstMethods;

    public Map<String, String> idxToPath;
    public Map<String, String> idxToSrcMethod;
    public Map<String, String> idxToDstMethod;


    public CrossFileRevisionAnalysis(String project, String commitId, Boolean isMultiple) throws Exception {
//        System.out.println("===================================");
//        System.out.println("Commit: " + commitId);
//        System.out.println("===================================");
        String baseCommitId = GitUtils.getBaseCommitId(project, commitId);
        Map<String, String> pathMap = null;
        try {
            pathMap = GitInfoRetrieval.getOldModifiedFileMap(project, commitId);
        } catch (Exception e) {
            pathMap = null;
            return;
        }

        this.methodDeclarationNum = 0;
        this.crossTransferMethodDeclarationNum = 0;

        this.srcPathToMethodsMap = new HashMap<>();
        this.dstPathToMethodsMap = new HashMap<>();

        idxToSrcMethod = new HashMap<>();
        idxToDstMethod = new HashMap<>();
        idxToPath = new HashMap<>();

        List<String> srcMethodsList;
        List<String> dstMethodsList;

        allDstMethods = new ArrayList<>();

        Map<String, Map<String, Double>> mpSimMap = new HashMap<>();

        Map<String, Double> srcMethodToSimCompare = new HashMap<>();

        Map<String, String> dstMethodToPath = new HashMap<>();
        Map<String, String> dstMethodToIndex = new HashMap<>();


        int idx = 0;
        for (Map.Entry<String, String> entry : pathMap.entrySet()) {
            String srcPath = entry.getKey();
            String dstPath = entry.getValue();

            // get Content for both src and dst
            ByteArrayOutputStream srcFileStream = GitUtils
                    .getFileContentOfCommitFile(project, baseCommitId, srcPath);
            srcFileContent = srcFileStream.toString("UTF-8");
            if (srcFileContent.equals("")) {
                srcFileContent = null;
                return;
            }
            ByteArrayOutputStream dstFileStream = GitUtils
                    .getFileContentOfCommitFile(project, commitId, dstPath);
            dstFileContent = dstFileStream.toString("UTF-8");
            if (dstFileContent.equals("")) {
                dstFileContent = null;
                return;
            }

            // generate src_tree, and dst_tree
            ITree tmp_src = null;
            ITree tmp_dst = null;
            try {
                tmp_src = GumTreeUtil.getITreeRoot(srcFileContent, used_ASTType);
                tmp_dst = GumTreeUtil.getITreeRoot(dstFileContent, used_ASTType);
            } catch (Exception e) {
                break;
            }
            // fresh the method list for each srcPath
            srcMethodsList = new ArrayList<>();
            dstMethodsList = new ArrayList<>();

            // get all method declarations
            srcMethodsList = DeclarationUtil.getMethodDeclarations(tmp_src);
            dstMethodsList = DeclarationUtil.getMethodDeclarations(tmp_dst);

            // format printer step2:
            System.out.println("=================");
            System.out.println("File: " + srcPath);
            System.out.println("=================");
            System.out.println("The total number of methods is " + srcMethodsList.size() + ": ");
            for (String srcMethodDeclaration : srcMethodsList) {
                System.out.println(srcMethodDeclaration);
            }

            this.methodDeclarationNum += srcMethodsList.size();

            List<String> commonMethods = new ArrayList<>(srcMethodsList);
            commonMethods.retainAll(dstMethodsList);
            List<String> filterSrcMethodList = new ArrayList<>(srcMethodsList);
            List<String> filterDstMethodList = new ArrayList<>(dstMethodsList);
            filterSrcMethodList.removeAll(commonMethods);
            filterDstMethodList.removeAll(commonMethods);

            srcPathToMethodsMap.put(srcPath, filterSrcMethodList);
            dstPathToMethodsMap.put(dstPath, filterDstMethodList);

            int idz = 0;
            for (String filterDstMethod : filterDstMethodList) {
                dstMethodToPath.put(filterDstMethod, dstPath);
                String dstIndex = idx + "-" + idz++;
                dstMethodToIndex.put(filterDstMethod, dstIndex);
            }
            allDstMethods.addAll(filterDstMethodList);

            int idy = 0;
            for (String srcMethod : filterSrcMethodList) {
                String srcIndex = idx + "-" + idy;
                Map<String, Double> methodToSimMap = new HashMap<>();
                for (String dstMethod : filterDstMethodList) {
                    String dstIndex = dstMethodToIndex.get(dstMethod);
                    double similarity = compareTwo(srcMethod, dstMethod);
                    methodToSimMap.put(dstIndex, similarity);
                    srcMethodToSimCompare.put(srcMethod, similarity);
                    Double max_similarity = srcMethodToSimCompare.getOrDefault(srcMethod, 0.0);
                    if (max_similarity <= similarity) {
                        methodToSimMap.clear();
                        methodToSimMap.put(dstIndex, similarity);
                        srcMethodToSimCompare.put(srcMethod, similarity);
                    }
                    idxToPath.put(srcIndex, srcPath);
                    idxToSrcMethod.put(srcIndex, srcMethod);
                    idxToDstMethod.put(dstIndex, dstMethod);
                    dstMethodToIndex.put(dstMethod, dstIndex);
                }
                mpSimMap.put(srcIndex, methodToSimMap);
                idy++;
            }
            idx++;
        }


        idx = 0;
        String srcIndex;
        for (Map.Entry<String, List<String>> entry : srcPathToMethodsMap.entrySet()) {
            String srcPath = entry.getKey();
            List<String> srcMethodList = entry.getValue();
            int idy = 0;

            List<String> curDstMethods = dstPathToMethodsMap.get(srcPath);
            List<String> crossDstMethods = new ArrayList<>(allDstMethods);
            crossDstMethods.removeAll(curDstMethods);

            for (String srcMethod : srcMethodList) {
                srcIndex = idx + "-" + idy;
                Map<String, Double> methodToSimMap = mpSimMap.get(srcIndex);
                if (methodToSimMap == null) methodToSimMap = new HashMap<>();
                for (String dstMethod : crossDstMethods) {
                    String dstIndex = dstMethodToIndex.get(dstMethod);
                    double cross_similarity = compareTwo(srcMethod, dstMethod);
                    methodToSimMap.put(dstIndex, cross_similarity);
                    idxToPath.put(srcIndex, srcPath);
                    idxToSrcMethod.put(srcIndex, srcMethod);
                    idxToDstMethod.put(dstIndex, dstMethod);
                }
                mpSimMap.put(srcIndex, methodToSimMap);
                idy ++;
            }
            idx ++;
        }

        double threshold = 0.8;
        for (Map.Entry<String, Map<String, Double>> entry : mpSimMap.entrySet()){
            srcIndex = entry.getKey();
            Map<String, Double> tmpmethodToSimMap = entry.getValue();
            double maxSim = 0.0;
            String subOptimDstIndex = null;
            String srcPath = idxToPath.get(srcIndex);
            int flag = 0;
            for (String dstIndex : tmpmethodToSimMap.keySet()){
                double sim = tmpmethodToSimMap.get(dstIndex);
                if (sim >= threshold){
                    String dstMethod = idxToDstMethod.get(dstIndex);
                    String srcMethod = idxToSrcMethod.get(srcIndex);
                    String dstPath = dstMethodToPath.get(dstMethod);
                    if (!dstPath.equals(srcPath)){
                        if (!isMultiple) {
                            if (sim >= maxSim) {
                                maxSim = sim;
                                subOptimDstIndex = dstIndex;
                            }
                        }
                        else {
                            String crossFileMapping = "In file: " + srcPath + "\n" + "[" + srcMethod + "] ----> "
                                    + "\n" + dstMethodToPath.get(dstMethod) + "-[" + dstMethod + "]"
                                    + "\n" + "Similarity: " + sim + "\n\n";
                            crossFileMappingList.add(crossFileMapping);
                            flag = 1;
                            if (flag == 1) {
                                this.crossTransferMethodDeclarationNum += 1;
                            }
                        }
                    }

                }
            }
            if (!isMultiple){
                if (maxSim > 0){
                    String dstMethod = idxToDstMethod.get(subOptimDstIndex);
                    String srcMethod = idxToSrcMethod.get(srcIndex);
                    String crossFileMapping = "In file: " + srcPath + "\n" + "[" + srcMethod + "] ----> "
                            + "\n" + dstMethodToPath.get(dstMethod) + "-[" + dstMethod + "]"
                            + "\n" + "Similarity: " + maxSim + "\n\n";
                    crossFileMappingList.add(crossFileMapping);
                    flag = 1;
                }
                if (flag == 1) {
                    this.crossTransferMethodDeclarationNum += 1;
                }
            }
        }
    }

    /**
     * public static double compareTwo(ITree src)
     * public static double compareTwo(Tree src)
     *
     * @param src
     * @param dst
     * @return
     */
    public static double compareTwo(String src, String dst) {
        String[] srcTokens = src.split("\\s+");
        String[] dstTokens = dst.split("\\s+");
        Map<CharSequence, Integer> srcMaptokens = convertToMap(srcTokens);
        Map<CharSequence, Integer> dstMaptokens = convertToMap(dstTokens);
        CosSimilarity simEngine = new CosSimilarity();
        double comparisonSimilarity = simEngine.cosineSimilarity(srcMaptokens, dstMaptokens);
        return comparisonSimilarity;
    }

    private static Map<CharSequence, Integer> convertToMap(String[] tokens) {
        Map<CharSequence, Integer> map = new HashMap<>();
        for (String token : tokens) {
            map.put(token, map.getOrDefault(token, 0) + 1);
        }
        return map;
    }

    public static List<String> getCommitList(String project, String filePath) {
        List<String> commitList = new ArrayList<>();
        try {
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
//                System.out.println(666);
                commitList.add(line.split(" ")[0].trim());
            }
            br.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return commitList;

    }

    public static void main(String[] args) throws Exception {
        /**
         * test point for one commit
         * commit id: 0d529af312c9bc1bd0560e67b549f0012cda5937
         * 7077d2b910405dea7a60c5140824966ffebc66a8 Syntax error on source code
         * 58aca869816e893e7a2f34f0708c1d7fcbdca0f5 98% -> 97% -> 43% -> 22%
         * 0c93dfde72494a5906b2937d320bde8ef46794e6 22% -> 0%
         * 8f55d404affc0e4ab556ae1937a1ff8d21cdb368
         * fb3b6dba571b9dbffaac45ac920037760ceb6dbc(342)
         * 8d11f07a96fe4e2a0a338e68c9785438813d53b6
         * project name: activemq
         */
        System.out.println("===================================");
        String commitID = "8d11f07a96fe4e2a0a338e68c9785438813d53b6";
        System.out.println("Commit: " + commitID);
        System.out.println("===================================");
        CrossFileRevisionAnalysis instance = new CrossFileRevisionAnalysis("activemq", commitID, false);
        System.out.println(instance.methodDeclarationNum);
        System.out.println(instance.crossTransferMethodDeclarationNum);
        System.out.println("The rate of cross transfer method is: ");
        System.out.println((100 * instance.crossTransferMethodDeclarationNum) / instance.methodDeclarationNum + "%");

        System.out.println("=============================");
        for (String crossFileMapping : instance.crossFileMappingList) {
            System.out.println(crossFileMapping);
        }
        System.out.println("=============================");

//        /**
//         * test point for one project
//         * project name: activemq
//         */
//        String project = "activemq";
//        String filePath = "ase2023" + File.separator + "project_commits-1" + File.separator + project + ".txt";
//        List<String> commitList = getCommitList(project,filePath);
//        // transfer commitList to set
//        Set<String> commitSet = new HashSet<>(commitList);
////        System.out.println(commitSet.size());
////        System.out.println(commitSet);
//        // only remain 100 elements in commitSet
//        while (commitSet.size() > 2000){
//            commitSet.remove(commitSet.iterator().next());
//        }
//        Integer totalCrossTransferMethodDeclarationNum = 0;
//        Integer totalMethodDeclarationNum = 0;
//        for (String commitId : commitSet){
//            if(!commitId.equals("58aca869816e893e7a2f34f0708c1d7fcbdca0f5") && !commitId.equals("a9223e42ebc04d420b2e62e9e57450408ee9f513")){
//                CrossFileRevisionAnalysis instanceTmp = new CrossFileRevisionAnalysis("activemq", commitId);
//                if (instanceTmp.srcFilePathMap == null) {
//                    continue;
//                }
//                totalCrossTransferMethodDeclarationNum += instanceTmp.crossTransferMethodDeclarationNum;
//                totalMethodDeclarationNum += instanceTmp.methodDeclarationNum;
//
//                System.out.println("=============================");
//
//                System.out.println((instanceTmp.methodDeclarationNum != 0 ? ((100 * instanceTmp.crossTransferMethodDeclarationNum) / instanceTmp.methodDeclarationNum) : "0") + "%");
//                System.out.println("=============================");
//                for (String s : instanceTmp.crossList) {
//                    System.out.println(s);
//                }
//                System.out.println(commitId + " finished");
//            }
//        }
//        System.out.println("=============================");
//        System.out.println("The total rate of cross transfer method is: ");
//        System.out.println((100*totalCrossTransferMethodDeclarationNum) / totalMethodDeclarationNum +"%");
//        System.out.println("=============================");
    }
}
