package com.zuehlke.jgit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

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
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JgitTest extends AbstractJgitTest {
    
    protected void logCommit(final RevCommit commit) {
        System.out.println("Commit");
        System.out.println("commit.name: " + commit.getName());
        System.out.println("commit.message: " + commit.getFullMessage());
        System.out.println("commit.author.name: " + commit.getAuthorIdent().getName() + " (" + commit.getAuthorIdent().getEmailAddress()
                + ")");
        System.out.println("commit.committer.name: " + commit.getCommitterIdent().getName() + " ("
                + commit.getCommitterIdent().getEmailAddress()
                + ")");
    }
    
    protected CredentialsProvider getCredentialsProvider() {
        final Properties prop = new Properties();
        
        try {
            // load a properties file
            prop.load(new FileInputStream("config.properties"));
            
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        
        final String gitUser = prop.getProperty("gituser");
        final String gitPassword = prop.getProperty("gitpassword");
        
        final CredentialsProvider cp = new UsernamePasswordCredentialsProvider(gitUser, gitPassword);
        return cp;
    }
    
    @Test
    public void testACreate() throws IOException {
        
        final Repository newRepo = new FileRepository(localPath + ".git");
        newRepo.create();
        newRepo.close();
        
        assertNotNull("newRepo is null", newRepo);
    }
    
    @Test
    public void testBClone() throws IOException, InvalidRemoteException, TransportException, GitAPIException {
        final Git gitObject = Git.cloneRepository()
                                 .setURI(remotePath)
                                 .setDirectory(new File(localPath))
                                 .call();
        
        gitObject.getRepository()
                 .close();
        
        assertNotNull("gitObject is null", gitObject);
    }
    
    @Test
    public void testCAddNewFile() throws IOException, GitAPIException {
        final File myfile = new File(localPath + "/myfile");
        myfile.createNewFile();
        final DirCache dirCache = git.add()
                                     .addFilepattern("myfile")
                                     .call();
        
        assertNotNull("dirCache is null", dirCache);
    }
    
    @Test
    public void testDCommitNewFile() throws IOException, JGitInternalException,
            UnmergedPathsException, GitAPIException {
        final Date now = new Date();
        final RevCommit commit = git.commit()
                                    .setMessage("Testrun at " + now.toString() + ": added myfile")
                                    .call();
        assertNotNull("commit is null", commit);
        
        logCommit(commit);
    }
    
    @Test
    public void testEPush() throws IOException, JGitInternalException, TransportException, GitAPIException {
        final CredentialsProvider cp = getCredentialsProvider();
        
        final Iterable<PushResult> result = git.push()
                                               .setCredentialsProvider(cp)
                                               .call();
        
        assertNotNull("push result is null", result);
    }
    
    @Test
    public void testEPushRemoveNewFile() throws InvalidRemoteException, TransportException, GitAPIException {
        final DirCache dirCache = git.rm()
                                     .addFilepattern("myfile")
                                     .call();
        
        assertNotNull("dirCache is null", dirCache);
        
        // commit
        final Date now = new Date();
        final RevCommit commit = git.commit()
                                    .setMessage("Testrun at " + now.toString() + ": removed myfile")
                                    .call();
        assertNotNull("commit is null", commit);
        
        logCommit(commit);
        
        // Push
        final CredentialsProvider cp = getCredentialsProvider();
        
        final Iterable<PushResult> result = git.push()
                                               .setCredentialsProvider(cp)
                                               .call();
        
        assertNotNull("push result is null", result);
    }
    
    @Test
    public void testFTrackMaster() throws IOException, JGitInternalException, GitAPIException {
        final Ref branch = git.branchCreate()
                              .setName("testBranch")
                              .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
                              .setStartPoint("origin/master")
                              .setForce(true)
                              .call();
        
        assertNotNull("branch is null", branch);
    }
    
    @Test
    public void testGPull() throws IOException, TransportException, GitAPIException {
        final PullResult pull = git.pull()
                                   .call();
        
        assertNotNull("pull is null", pull);
    }
    
    @Test
    public void testHCommitWithDifferentUserConfig() throws IOException, JGitInternalException,
            UnmergedPathsException, GitAPIException {
        
        final Repository repo = git.getRepository();
        
        final String wasEmail = repo.getConfig().getString("user", null, "email");
        final String wasName = repo.getConfig().getString("user", null, "name");
        
        // change user and mail
        final String email = "john.locke@815.com";
        repo.getConfig().setString("user", null, "email", email);
        final String name = "John Locke";
        repo.getConfig().setString("user", null, "name", name);
        
        final RevCommit commit = git.commit()
                                    .setMessage("Added myfile")
                                    .call();
        
        assertNotNull("commit is null", commit);
        assertEquals("author name does not match", name, commit.getAuthorIdent().getName());
        assertEquals("author email does not match", email, commit.getAuthorIdent().getEmailAddress());
        assertEquals("committer name does not match", name, commit.getCommitterIdent().getName());
        assertEquals("committer email does not match", email, commit.getCommitterIdent().getEmailAddress());
        
        logCommit(commit);
        
        // reset initial user and name
        repo.getConfig().setString("user", null, "email", wasEmail);
        repo.getConfig().setString("user", null, "name", wasName);
    }
    
    @Test
    public void testHCommitWithDifferentCommitAuthorConfig() throws IOException, JGitInternalException,
            UnmergedPathsException, GitAPIException {
        
        // change user and mail
        final String email = "john.locke@815.com";
        final String name = "John Locke";
        final RevCommit commit = git.commit()
                                    .setMessage("Added myfile")
                                    .setAuthor(name, email)
                                    .call();
        
        assertNotNull("commit is null", commit);
        assertEquals("author name does not match", name, commit.getAuthorIdent().getName());
        assertEquals("author email does not match", email, commit.getAuthorIdent().getEmailAddress());
        assertNotEquals("committer name does match", name, commit.getCommitterIdent().getName());
        assertNotEquals("committer email does match", email, commit.getCommitterIdent().getEmailAddress());
        
        logCommit(commit);
    }
}
