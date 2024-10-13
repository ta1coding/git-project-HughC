import java.io.File;
import java.io.IOException;

public class GitTester {
    // The tester requires the specific hashes generated for each commit in order to
    // checkout. Testing the checkout funcitonality requires creating a repo,
    // staging, committing, then wiping the working directory. Afterwards, it is
    // possible to locate the hash of the commit and use the checkout function to
    // restore it. I have tested it and it works. Here is an example of what testing
    // the code might look like.
    public static void main(String[] args) throws IOException {
        // Git git = new Git();
        // git.stage("test");
        // git.commit("me", "test commit");
        // wipe(new File("test"));
        // git.checkout("3e5df803ccce41485ee4517792fb9e5d413046bb");
    }

    // recursively remove a file
    public static void wipe(File file) throws IOException {
        if (file.exists() && file.isDirectory())
            for (File f : file.listFiles())
                wipe(f);
        file.delete();
    }

    // checks if all correct files exist in their propper locations and then deletes
    // them after

    public static void checkAndDelete() throws IOException {
        File gitDir = new File("git");

        File objectsDir = new File("git/objects");

        File indexFile = new File("git/index");

        if (gitDir.exists() && objectsDir.exists() && indexFile.exists()) {
            System.out.println("Git repository exists");
        } else {
            if (!gitDir.exists()) {
                System.out.println("git directory doesnt exist");
            }
            if (!objectsDir.exists()) {
                System.out.println("objects directory doesnt exist");
            }
            if (!indexFile.exists()) {
                System.out.println("index doesnt exist");
            }
        }

        System.out.println("now deleting:");

        objectsDir.delete();
        indexFile.delete();
        gitDir.delete();
    }
}
