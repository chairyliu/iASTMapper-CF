package cs.model.gitops;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * APIs to process RevCommit object in Jgit.
 */
public class CommitOps {

    public static String getCommitId(RevCommit rc){
        return rc.getId().getName();
    }

    public static int getCommitTime(RevCommit rc){
        return rc.getCommitTime();
    }

}
