package com.zuehlke.jgit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class AbstractJgitTest {
    
    protected static String localPath = System.getProperty("java.io.tmpdir") + "jgittest/testrepo";
    protected String remotePath;
    protected Repository localRepo;
    protected Git git;
    
    public static void deleteFolder(final File folder) {
        final File[] files = folder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (final File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    System.out.println("delete " + f.getAbsolutePath() + " : " + f.delete());
                }
            }
        }
        folder.delete();
    }
    
    @Before
    public void setUp() throws IOException {
        remotePath = getRemoteRepoUri();
        localRepo = new FileRepository(localPath + "/.git");
        git = new Git(localRepo);
    }
    
    @After
    public void tearDownAfter() {
        localRepo.close();
        localRepo = null;
        git.close();
        git = null;
    }
    
    @AfterClass
    @BeforeClass
    public static void tearDown() {
        
        final File existingLocalRepo = new File(localPath + "/.git");
        if (existingLocalRepo.exists()) {
            deleteFolder(existingLocalRepo);
        }
        
        final File testDir = new File(localPath);
        if (testDir.exists()) {
            deleteFolder(testDir);
        }
        
    }
    
    protected String getRemoteRepoUri() {
        String repoUri = "https://github.com/bej/jgit-testrepo.git";
        final Properties prop = new Properties();
        try {
            // load a properties file
            prop.load(new FileInputStream("config.properties"));
            repoUri = prop.getProperty("gitrepository");
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        return repoUri;
    }
    
}
