package ase2023;

import cs.model.gitops.CommitOps;
import cs.model.gitops.GitService;
import cs.model.gitops.GitUtils;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.*;
import java.util.*;

public class GitProjectInfoExtractor {

    static Map<String, Set<String>> commitIdToFileMap = new HashMap<>();

    public static Map<String, Set<String>> readAnalyzedCommits(String RQ1_file){

        commitIdToFileMap.clear();
        String line = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(RQ1_file));
            while((line = br.readLine()) != null) {
                if (line.trim().equals(""))
                    continue;
                String[] sa = line.split("\\|")[0].trim().split(" ");
                String project = sa[0].trim().split("\\\\")[0];
                String javaFile = sa[0].trim().substring(project.length()+1);
                sa = sa[1].trim().split("/");
                String commitId = sa[sa.length-1];
                if (!commitIdToFileMap.containsKey(commitId))
                    commitIdToFileMap.put(commitId, new HashSet<>());
                commitIdToFileMap.get(commitId).add(javaFile.replace("\\", "/"));
            }
            br.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(commitIdToFileMap.size());
        return commitIdToFileMap;
    }

    static List<String> addedFiles = new ArrayList<>();

    static Map<String, String> oldModifyMap = new HashMap<>();


    /**
     * Extract the analyzed commits of a project.
     */
    public static void extractForOneProject(String project, String commitsFile){

        addedFiles.clear();
        oldModifyMap.clear();
        GitService gitService = GitUtils.getRawGitService(project);
        Set<String> C = new HashSet<>();
        int frN = 0;

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(commitsFile));
            RevWalk revWalk = GitUtils.getRevWalkForAllBranches(gitService);
            Iterator<RevCommit> iter = revWalk.iterator();

            while(iter.hasNext()){
                RevCommit tmp = iter.next();
                String commitId = CommitOps.getCommitId(tmp);
                if (!commitIdToFileMap.containsKey(commitId))
                    continue;
                if (tmp.getParentCount() > 0){
                    RevCommit baseCommit = tmp.getParent(0);
                    if (baseCommit != null) {
                        String baseCommitId = CommitOps.getCommitId(baseCommit);
                        gitService.getFileModificationInfo(tmp, baseCommit, addedFiles, oldModifyMap);
                        if (oldModifyMap.size() > 0) {
                            for (String oldPath: oldModifyMap.keySet()) {
                                if (commitIdToFileMap.get(commitId).contains(oldPath)) {
                                    frN++;
                                    C.add(commitId);
                                    String newPath = oldModifyMap.get(oldPath);
                                    bw.write(commitId + " " + baseCommitId + " " + oldPath + " " + newPath + "\n");
                                }
                            }
                        }
                    }
                }
            }
            bw.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(project + " -> #Commits: " + C.size() + ", #FRs: " + frN);
    }

    public static void createPath(String path){
        File fo = new File(path);
        if (!fo.exists())
            fo.mkdirs();
    }


    /**
     *
     * @param args
     */
    public static void main(String[] args) {


        String RQ1_path = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\RQ1";
        String project_commits_path = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\project_commits-1";
        createPath(project_commits_path);

        String[] projects = new String[]{ "activemq", "commons-io", "commons-lang", "commons-math",
                "hibernate-orm", "hibernate-search", "junit4", "netty", "spring-framework", "spring-roo" };

        for(String project : projects) {
            String RQ1_file = RQ1_path + "\\" + project + ".txt";
            String commits_file = project_commits_path + "\\" + project + ".txt";
            readAnalyzedCommits(RQ1_file);
            extractForOneProject(project, commits_file);
        }
    }

}
