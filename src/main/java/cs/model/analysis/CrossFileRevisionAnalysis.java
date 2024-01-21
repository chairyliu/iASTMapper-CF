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
    public List<String> crossList = new ArrayList<>();
    public Integer crossTransferMethodDeclarationNum;
    public Map<String, List<String>> srcPathToMethodsMap;
    public Map<String, List<String>> dstPathToMethodsMap;
    public List<String> allDstMethods;

    public Map<String, String> idx2Path;
    public Map<String, String> idx2Method;

    public CrossFileRevisionAnalysis(String project, String commitId) throws Exception {
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

        //所有srcpath下的方法总数，也就是分母
        this.methodDeclarationNum = 0;
        //存在跨文件映射的方法，也就是分子
        this.crossTransferMethodDeclarationNum = 0;

        //srcPathToMethodsMap中第一个String代表srcpath，第二个List存放每一个srcpath下的所有改动过的方法声明，把所有方法汇总起来存到map里，dst同理
        this.srcPathToMethodsMap = new HashMap<>();
        this.dstPathToMethodsMap = new HashMap<>();

        //这两个参数分别用来存每个索引数字对应的srcpath和srcmethod
        idx2Method = new HashMap<>();
        idx2Path = new HashMap<>();

        List<String> srcMethodsList;
        List<String> dstMethodsList;

        //因为在跨文件映射的时候，需要srcpath下的方法和其非对应dstpath下的所有方法比较，如果直接用dstPathAndMethodsMap，
        //除了遍历路径外，还需要遍历每个list，会进行多层循环。所以用allDstMethods存储所有dstpath下的方法，减少了循环次数
        allDstMethods = new ArrayList<>();

        //第一个String表示srcpath-srcmethod索引，第二个map集合绑定了每个dst方法和其相似度值，方便比较同一个方法同文件映射和跨文件映射的相似度大小
        Map<String, Map<String, Double>> mpSimMap = new HashMap<>();

        Map<String, Double> srcMethodToSimCompare = new HashMap<>();

        //第一个参数是dst方法，第二个参数是其对应的dstpath。因为后续输出时，需要拿到每一个dst方法对应的路径
        Map<String, String> reverseProjectMap = new HashMap<>();

        int idx = 0;
        //这一块总体是遍历每个srcpath和dstpath，拿到其AST树和每个路径下的所有方法。再把ITree节点转换成字符串，存入对应的map集合中
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
//            int t = 0;
            System.out.println("The total number of methods is " + srcMethodsList.size() + ": ");
            for (String srcMethodDeclaration : srcMethodsList) {
                System.out.println(srcMethodDeclaration);
            }

            // 每拿完一个srcpath下的所有方法，都会更新methodDeclarationNum的值
            this.methodDeclarationNum += srcMethodsList.size();

            //过滤掉没有修改的方法。commonMethods表示src和dst里相同的方法，如果两个方法完全相同，那么就说明没有更改过，不考虑未修改的方法
            List<String> commonMethods = new ArrayList<>(srcMethodsList);
            commonMethods.retainAll(dstMethodsList);
            //filterSrcMethodList表示已经过滤后的方法集合，只包含修改过的语句
            List<String> filterSrcMethodList = new ArrayList<>(srcMethodsList);
            List<String> filterDstMethodList = new ArrayList<>(dstMethodsList);
            filterSrcMethodList.removeAll(commonMethods);
            filterDstMethodList.removeAll(commonMethods);
//            System.out.println("6");

            srcPathToMethodsMap.put(srcPath, filterSrcMethodList);
            dstPathToMethodsMap.put(dstPath, filterDstMethodList);

            //对于每个dst中的方法，都将它和其dstpath绑定起来，方便后面输出
            for (String filterDstMethod : filterDstMethodList) {
                reverseProjectMap.put(filterDstMethod, dstPath);
            }
            //每一次方法过滤后，都同步更新allDstMethods列表，所有dstpath都遍历一遍后，该参数内存着所有修改过的dst方法
            allDstMethods.addAll(filterDstMethodList);

            int idy = 0;
            for (String srcMethod : filterSrcMethodList) {
                String index = idx + "-" + idy;
                Map<String, Double> methodToSimMap = new HashMap<>();
                for (String dstMethod : filterDstMethodList) {
                    //src和dst路径相同时，获取每个方法之间的相似度
                    double similarity = compareTwo(srcMethod, dstMethod);
                    //mpSimMap的第二个参数，将每个dst方法和其相似度值绑定
                    methodToSimMap.put(dstMethod, similarity);
                    //将src的方法和相似度绑定，方便后面获取每个srcmethod对应的最大相似度值
                    srcMethodToSimCompare.put(srcMethod, similarity);
//                    System.out.println("5");
                    //定义max_similarity用来更迭src方法最大的相似度
                    Double max_similarity = srcMethodToSimCompare.getOrDefault(srcMethod, 0.0);
                    if (max_similarity <= similarity) {
                        methodToSimMap.clear();
                        methodToSimMap.put(dstMethod, similarity);
                        srcMethodToSimCompare.put(srcMethod, similarity);
//                        System.out.println("123");
                    }
                    idx2Path.put(index, srcPath);
                    idx2Method.put(index, srcMethod);
                }
                mpSimMap.put(index, methodToSimMap);
                idy++;
            }
            //map<path, mpSImMap>
            //map<srcpath ,map<srcmethod ,map<map<dstpath, map<dstmethod ,sim>>>>
            idx++;
        }



        idx = 0;
        String index;
        for (Map.Entry<String, List<String>> entry : srcPathToMethodsMap.entrySet()) {
            String srcPath = entry.getKey();
            List<String> srcMethodList = entry.getValue();
            int idy = 0;
            //获取当前dstpath下所有修改过的方法
            List<String> curDstMethods = dstPathToMethodsMap.get(srcPath);
            //初始化一个crossDstMethods，现在里面存了所有dstpath下的所有修改方法
            List<String> crossDstMethods = new ArrayList<>(allDstMethods);
            //从crossDstMethods中移除掉当前dstpath下的修改方法，剩下的方法列表就是用于跨文件映射的方法
            crossDstMethods.removeAll(curDstMethods);

            for (String srcMethod : srcMethodList) {
                index = idx + "-" + idy;
                Map<String, Double> methodToSimMap = mpSimMap.get(index);
                if (methodToSimMap == null) methodToSimMap = new HashMap<>();
                for (String dstMethod : crossDstMethods) {
                    double cross_similarity = compareTwo(srcMethod, dstMethod);
                    methodToSimMap.put(dstMethod, cross_similarity);
                    //System.out.println("6");
                    idx2Path.put(index, srcPath);
                    idx2Method.put(index, srcMethod);
                }
                mpSimMap.put(index, methodToSimMap);
                idy ++;
            }
            idx ++;
        }

        double threshold = 0.8;
        for (Map.Entry<String, Map<String, Double>> entry : mpSimMap.entrySet()){
            index = entry.getKey();
            Map<String, Double> tmpmethodToSimMap = entry.getValue();
            double maxSim = 0.0;
            for (String dstMethod : tmpmethodToSimMap.keySet()){
                int flag = 0;
                double sim = tmpmethodToSimMap.get(dstMethod);
                if (sim >= threshold){
                    //拿到dstMethod对应的dstpath，如果和索引中srcmethod的srcpath一致，则不是跨文件，跳过，若不一致，跨文件+1
                    String dstPath = reverseProjectMap.get(dstMethod);
                    String srcPath = idx2Path.get(index);
                    String srcMethod = idx2Method.get(index);
                    if (!dstPath.equals(srcPath)){
                        String r = "In file: " + srcPath + "\n" + "[" + srcMethod + "] ----> "
                                + "\n" + reverseProjectMap.get(dstMethod) + "-[" + dstMethod + "]"
                                + "\n" + "Similarity: " + sim + "\n\n";
//                        System.out.print(r);
                        //存储了所有跨文件映射的方法，用于后续集体输出
                        crossList.add(r);
                        flag = 1;
                    }
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
        CrossFileRevisionAnalysis instance = new CrossFileRevisionAnalysis("activemq", commitID);
        System.out.println(instance.methodDeclarationNum);
        System.out.println(instance.crossTransferMethodDeclarationNum);
        System.out.println("The rate of cross transfer method is: ");
        System.out.println((100 * instance.crossTransferMethodDeclarationNum) / instance.methodDeclarationNum + "%");

        System.out.println("=============================");
        for (String s : instance.crossList) {
            System.out.println(s);
        }
        System.out.println("=============================");

        /**
         * test point for one project
         * project name: activemq
         */
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
