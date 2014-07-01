package com.zuehlke.jgit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Test;

public class NestedRepositoryTest extends AbstractJgitTest {
    
    protected String nestedRepoPath = Paths.get(localPath, "nested").toString();
    protected String nestedRemotePath = "https://github.com/bej/cookbook.git";
    
    protected void printStatus(final Status status) throws NoWorkTreeException, GitAPIException {
        System.out.println("Added: " + status.getAdded());
        System.out.println("Changed: " + status.getChanged());
        System.out.println("Conflicting: " + status.getConflicting());
        System.out.println("ConflictingStageState: " + status.getConflictingStageState());
        System.out.println("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
        System.out.println("Missing: " + status.getMissing());
        System.out.println("Modified: " + status.getModified());
        System.out.println("Removed: " + status.getRemoved());
        System.out.println("Untracked: " + status.getUntracked());
        System.out.println("UntrackedFolders: " + status.getUntrackedFolders());
    }
    
    @Test
    public void testNestedRepository() throws InvalidRemoteException, TransportException, GitAPIException, IOException {
        // clone the parent repository
        System.out.println("clone repository to " + localPath);
        final Git parentGit = Git.cloneRepository()
                                 .setURI(remotePath)
                                 .setDirectory(new File(localPath))
                                 .call();
        
        assertNotNull("parentGit is null", parentGit);
        
        // clone the nested repository
        System.out.println("clone nested repository to " + nestedRepoPath);
        final File nestedRepoDirectory = new File(nestedRepoPath);
        final Git nestedGit = Git.cloneRepository()
                                 .setURI(nestedRemotePath)
                                 .setDirectory(nestedRepoDirectory)
                                 .call();
        assertNotNull("nestedGit is null", nestedGit);
        
        parentGit.add().addFilepattern(nestedRepoDirectory.getName()).call();
        parentGit.commit().setMessage("added nested repository").call();
        // add the nested repository files to the parent repo
        
        Status parentStatus = parentGit.status().call();
        printStatus(parentStatus);
        assertTrue("file from nested repo is added to parentRepo", parentStatus.getAdded().isEmpty());
        
        // create a new file in the nested repo
        final File newFile = Paths.get(nestedRepoPath, "newFile.txt").toFile();
        assertFalse("File " + newFile.getAbsolutePath() + " does exist", newFile.exists());
        
        if (newFile.createNewFile()) {
            nestedGit.add().addFilepattern("newFile.txt").call();
            parentGit.add().addFilepattern("nested/newFile.txt").call();
        }
        
        final Status nestedStatus = nestedGit.status().call();
        parentStatus = parentGit.status().call();
        assertEquals("newFile is not added to nestedRepo", 1, nestedStatus.getAdded().size());
        assertEquals("newFile is added to parentRepo", 0, parentStatus.getAdded().size());
        assertTrue("newFile is tracked in parentRepo", parentStatus.getUntracked().isEmpty());
        
        System.out.println("status of parentGit");
        printStatus(parentStatus);
        System.out.println("status of nestedGit");
        printStatus(nestedStatus);
        
        listRepositoryContents(parentGit.getRepository());
        listRepositoryContents(nestedGit.getRepository());
    }
    
    private static void listRepositoryContents(final Repository repository) throws IOException {
        
        final Ref head = repository.getRef("HEAD");
        
        // a RevWalk allows to walk over commits based on some filtering that is
        // defined
        final RevWalk walk = new RevWalk(repository);
        
        final RevCommit commit = walk.parseCommit(head.getObjectId());
        final RevTree tree = commit.getTree();
        System.out.println("Having tree: " + tree + " " + commit.getFullMessage());
        
        // now use a TreeWalk to iterate over all files in the Tree recursively
        // you can set Filters to narrow down the results if needed
        final TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
            System.out.println("found: " + treeWalk.getPathString());
        }
        
    }
}
