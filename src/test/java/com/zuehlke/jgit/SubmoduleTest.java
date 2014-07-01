package com.zuehlke.jgit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleStatusType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SubmoduleTest {
    
    protected static String localPath = Paths.get(System.getProperty("java.io.tmpdir"), "jgittest").toString();
    protected static String localCookbookPath = Paths.get(localPath, "cookbook").toString();
    protected static String localRecipesCommonPath = Paths.get(localPath, "recipes-common").toString();
    private static final String COOKBOOK_URI = "https://github.com/bej/cookbook.git";
    private static final String RECIPES_COMMON_URI = "https://github.com/bej/recipes-common.git";
    
    private Repository localCookbookRepo = null;
    private Git cookbookGit = null;
    private Git recipesCommonGit = null;
    private Repository localRecipesCommonRepo = null;
    
    public static void deleteFolder(final File folder) {
        final File[] files = folder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (final File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
    
    @AfterClass
    @BeforeClass
    public static void tearDown() {
        
        final File testDir = new File(localPath);
        if (testDir.exists()) {
            deleteFolder(testDir);
        }
        
        final File existingLocalRepo = new File(localCookbookPath + ".git");
        if (existingLocalRepo.exists()) {
            deleteFolder(existingLocalRepo);
        }
    }
    
    @Before
    public void setUp() throws IOException {
        localCookbookRepo = new FileRepository(localCookbookPath + "/.git");
        cookbookGit = new Git(localCookbookRepo);
        
        localRecipesCommonRepo = new FileRepository(localRecipesCommonPath + "/.git");
        recipesCommonGit = new Git(localRecipesCommonRepo);
    }
    
    @Test
    public void test_A_cloneRepos() throws GitAPIException, InvalidRemoteException, TransportException, IOException {
        // clone the parent repository
        System.out.println("clone repository to " + localCookbookPath);
        Git gitObj = Git.cloneRepository()
                        .setURI(COOKBOOK_URI)
                        .setDirectory(new File(localCookbookPath))
                        .call();
        assertNotNull("cookbook repo is null", gitObj);
        
        gitObj = Git.cloneRepository()
                    .setURI(RECIPES_COMMON_URI)
                    .setDirectory(new File(localRecipesCommonPath))
                    .call();
        assertNotNull("recipes-common repo is null", gitObj);
        
        final File workDir = cookbookGit.getRepository().getWorkTree();
        final File readme = new File(workDir, "README.md");
        assertTrue("there is no README.md", readme.isFile());
    }
    
    @Test
    public void test_B_addSubmodule() throws InvalidRemoteException, TransportException, GitAPIException, IOException {
        // add recipes-common as submodule
        final String uri = recipesCommonGit.getRepository().getDirectory().getCanonicalPath();
        System.out.println("add recipes-common as submodule from " + uri);
        
        final SubmoduleAddCommand addCommand = cookbookGit.submoduleAdd();
        addCommand.setURI(uri);
        addCommand.setPath("recipes-common");
        
        final Repository recipesCommonRepo = addCommand.call();
        recipesCommonRepo.close();
        
        final File workDir = cookbookGit.getRepository().getWorkTree();
        final File submoduleDir = new File(workDir, "recipes-common");
        final File applepie = new File(workDir, "recipes-common/applepie");
        
        assertTrue("there is no directory recipes-common", submoduleDir.exists() && submoduleDir.isDirectory());
        assertTrue("there is no recipes-common/applepie", applepie.isFile());
    }
    
    @Test
    public void test_C_listSubmodules() throws GitAPIException {
        final Map<String, SubmoduleStatus> submodules = cookbookGit.submoduleStatus().call();
        
        assertFalse("there are no submodules", submodules.isEmpty());
        assertEquals("there is more or less than 1 submodule", 1, submodules.size());
        final SubmoduleStatus status = submodules.get("recipes-common");
        assertEquals("type of submodule is not INITIALIZED", SubmoduleStatusType.INITIALIZED, status.getType());
    }
    
    @Test
    public void test_D_updateSubmodule() throws NoHeadException, NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
            WrongRepositoryStateException, GitAPIException, IOException {
        final RevCommit commit = recipesCommonGit.commit().setMessage("test commit").call();
        
        final File workDir = cookbookGit.getRepository().getWorkTree();
        final Git submoduleGit = Git.open(new File(workDir, "recipes-common"));
        
        submoduleGit.pull().call();
        final ObjectId submoduleHead = submoduleGit.getRepository().resolve(Constants.HEAD);
        submoduleGit.getRepository().close();
        
        final ObjectId wasHeadId = cookbookGit.getRepository().resolve(Constants.HEAD);
        
        cookbookGit.add().addFilepattern("recipes-common").call();
        cookbookGit.commit().setMessage("Updated submodule").call();
        
        final ObjectId isHeadId = cookbookGit.getRepository().resolve(Constants.HEAD);
        
        assertNotEquals(wasHeadId, isHeadId);
        
        assertEquals("submodule is not at head revision", commit, submoduleHead);
    }
}
