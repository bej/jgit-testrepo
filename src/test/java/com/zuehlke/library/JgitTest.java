package com.zuehlke.library;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JgitTest {
    
    private static String localPath = System.getProperty("java.io.tmpdir") + "jgittest/testrepo";
    private String remotePath;
    private Repository localRepo;
    private Git git;
    
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
    
    @Before
    public void setUp() throws IOException {
        
        remotePath = "https://github.com/bej/jgit-testrepo.git";
        localRepo = new FileRepository(localPath + "/.git");
        git = new Git(localRepo);
    }
    
    @AfterClass
    @BeforeClass
    public static void tearDown() {
        
        File testDir = new File(localPath);
        if (testDir.exists()) {
            deleteFolder(testDir);
        }
        
        File existingLocalRepo = new File(localPath + ".git");
        if (existingLocalRepo.exists()) {
            deleteFolder(existingLocalRepo);
        }
        
    }
    
    @Test
    public void testACreate() throws IOException {
        
        final Repository newRepo = new FileRepository(localPath + ".git");
        newRepo.create();
        
        assertNotNull("newRepo is null", newRepo);
    }
    
    @Test
    public void testBClone() throws IOException, InvalidRemoteException, TransportException, GitAPIException {
        Git gitObject = Git.cloneRepository()
                           .setURI(remotePath)
                           .setDirectory(new File(localPath))
                           .call();
        
        assertNotNull("gitObject is null", gitObject);
    }
    
    @Test
    public void testCAdd() throws IOException, GitAPIException {
        final File myfile = new File(localPath + "/myfile");
        myfile.createNewFile();
        DirCache dirCache = git.add()
                               .addFilepattern("myfile")
                               .call();
        
        assertNotNull("dirCache is null", dirCache);
    }
    
    @Test
    public void testDCommit() throws IOException, JGitInternalException,
            UnmergedPathsException, GitAPIException {
        RevCommit commit = git.commit()
                              .setMessage("Added myfile")
                              .call();
        assertNotNull("commit is null", commit);
    }
    
    @Test
    @Ignore
    public void testEPush() throws IOException, JGitInternalException, TransportException, GitAPIException {
        git.push()
           .call();
    }
    
    @Test
    public void testFTrackMaster() throws IOException, JGitInternalException, GitAPIException {
        Ref branch = git.branchCreate()
                        .setName("testBranch")
                        .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
                        .setStartPoint("origin/master")
                        .setForce(true)
                        .call();
        
        assertNotNull("branch is null", branch);
    }
    
    @Test
    public void testGPull() throws IOException, TransportException, GitAPIException {
        PullResult pull = git.pull()
                             .call();
        
        assertNotNull("pull is null", pull);
    }
}
