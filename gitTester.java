import java.io.File;
import java.io.IOException;

public class gitTester {
    public static void main(String[] args) throws IOException {
        wipe(new File("git"));
        Git repo = new Git();
        File testFile = new File("testfile.txt");
        repo.makeBlob(testFile.getPath());
        File testDir = new File("testDir");
        repo.makeBlob(testDir.getPath());
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
