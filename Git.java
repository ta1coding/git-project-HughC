import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Git implements GitInterface {

    public static void main(String[] args) throws IOException {
        initGitRepo();
    }

    // constructor
    public Git() throws IOException {
        initGitRepo();
    }

    /**
     * Makes a Git Repo in the current folder
     * 
     * @throws IOException
     */
    private static void initGitRepo() throws IOException {

        File gitDir = new File("git");

        File objectsDir = new File("git/objects");

        File indexFile = new File("git/index");

        File headFile = new File("git/HEAD");

        File readMeFile = new File("README.md");

        // exist check
        if (gitDir.exists() && objectsDir.exists() && indexFile.exists() && headFile.exists() && readMeFile.exists()) {
            System.out.println("Git Repository already exists");
        } else {
            if (!gitDir.exists()) {
                gitDir.mkdir();
            }
            if (!objectsDir.exists()) {
                objectsDir.mkdir();
            }
            if (!indexFile.exists()) {
                indexFile.createNewFile();
            }
            if (!headFile.exists()) {
                headFile.createNewFile();
            }
            if (!readMeFile.exists()) {
                readMeFile.createNewFile();
            }
        }

        makeBlob("README.md", new ArrayList<>());

        commit("author", "Initial Commit", true);
    }

    /**
     * Commits the current staged changes.
     * 
     * @param author      - The author of the commit
     * @param description - The description of the commit
     * @param firstCommit - If the commit is the first one in the repo
     * @return The hash of the latest commit
     * @throws IOException
     */
    private static String commit(String author, String description, boolean firstCommit) throws IOException {
        StringBuilder commit = new StringBuilder();

        String tree = createCommitTree(firstCommit);

        String parent;
        if (!firstCommit)
            parent = getStringFromFile("git/HEAD");
        else
            parent = "";

        String date = getDate();

        commit.append("tree " + tree);
        commit.append("\n");

        commit.append("parent " + parent);
        commit.append("\n");

        commit.append("author " + author);
        commit.append("\n");

        commit.append("date " + date);
        commit.append("\n");

        commit.append("message " + description);
        commit.append("\n");

        String commitHash = generateFileHash(commit.toString().getBytes());
        updateHead(commitHash);

        BufferedWriter writer = new BufferedWriter(new FileWriter("git/objects/" + commitHash));
        writer.write(commit.toString());
        writer.close();

        resetFile("git/index");
        return commitHash;
    }

    /**
     * Updates the HEAD to store a new commit
     * 
     * @param commitHash - The hash of the commit
     * @throws IOException
     */
    private static void updateHead(String commitHash) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("git/HEAD"));
        writer.write(commitHash);
        writer.close();
    }

    /**
     * @return The current date and time formated in MM/dd/yyyy HH:mm:ss
     */
    private static String getDate() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dateTimeFormatter.format(now);
    }

    /**
     * Creates the tree file for the current commit.
     * 
     * @return The hash of the tree file created
     * @throws IOException
     */
    private static String createCommitTree(boolean firstCommit) throws IOException {
        String previousTree;
        if (!firstCommit)
            previousTree = getPreviousTree();
        else
            previousTree = "";
        StringBuilder tree = new StringBuilder(previousTree);
        String index = getStringFromFile("git/index");
        tree.append(index);
        String treeHash = generateFileHash(tree.toString().getBytes());
        BufferedWriter writer = new BufferedWriter(new FileWriter("git/objects/" + treeHash));
        writer.write(tree.toString());
        writer.close();
        return treeHash;
    }

    /**
     * Clears the contents of a file: makes the file empty
     * 
     * @param path - The path to the file
     * @throws IOException
     */
    private static void resetFile(String path) throws IOException {
        File file = new File(path);
        file.delete();
        file.createNewFile();
    }

    /**
     * Gets the previous tree in String form
     * 
     * @return The previous tree in String form
     * @throws IOException
     */
    private static String getPreviousTree() throws IOException {
        String commit = getStringFromFile("git/HEAD");
        String tree = commit.substring(5, 45);
        return tree;
    }

    /**
     * Gets the contents of a file in String form if applicable.
     * 
     * @param path The path of the file
     * @return The file in String form
     * @throws IOException
     */
    private static String getStringFromFile(String path) throws IOException {
        StringBuilder string = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        while (reader.ready()) {
            string.append(reader.readLine());
            string.append("\n");
        }
        reader.close();
        return string.toString();
    }

    /**
     * recursively generate pre-hashed tree file to be used in generateFileName
     * 
     * @param input - The folder to backup as a tree
     * @param files - An ArrayList of all the files in the tree
     * @return A tree as a byte array
     * @throws IOException
     */
    private static byte[] treeToBytes(File input, ArrayList<File> files) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (File f : input.listFiles()) {
            String fName;
            if (f.getAbsolutePath().equals(f.getCanonicalPath())) {
                fName = f.getName();
            } else {
                fName = f.getAbsolutePath();
            }
            if (f.isDirectory()) {
                if (files.contains(f)) {
                    throw new IllegalStateException("Cycle detected");
                }
                files.add(f);
                String fHash = generateFileHash(treeToBytes(f, files));
                sb.append("tree " + fHash + " " + fName + "\n");
            } else {
                String fHash = generateFileHash(Files.readAllBytes(Paths.get(f.getPath())));
                sb.append("blob " + fHash + " " + fName + "\n");
            }
        }
        return sb.toString().getBytes();
    }

    /**
     * Generates and returns the String version of a tree file.
     * 
     * @param input - the tree from which to retrieve the string version
     * @return The String version of a tree file
     * @throws IOException
     */
    private static String treeToString(File input) throws IOException {
        StringBuilder string = new StringBuilder();
        for (File file : input.listFiles()) {
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file.getPath()));
            byte[] bytes = new byte[(int) file.length()];
            inputStream.read(bytes);
            inputStream.close();
            String hash = generateFileHash(bytes);
            if (file.isFile()) {
                string.append("blob " + hash + " " + file.getPath() + "\n");
            } else {
                string.append("tree " + hash + " " + file.getPath() + "\n");
            }
        }
        return string.toString();
    }

    /**
     * Generates the hash string name according to SHA-1.
     * 
     * @param input - The byte data of the file in byte array form
     * @return The SHA-1 hash of the byte data
     * @throws IOException
     */
    private static String generateFileHash(byte[] input) throws IOException {
        try {

            // getInstance() method is called with algorithm SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input);

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding 0s to make it 40 digits long
            while (hashtext.length() < 40) {
                hashtext = "0" + hashtext;
            }

            // return the HashText
            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Makes a blob and stores it in objects
     * 
     * @param path - The path to the file
     * @return The hash of the file
     * @throws IOException
     * @throws FileNotFoundException
     * @throws IllegalStateException
     */
    public void stage(String path) {
        ArrayList<String> hashes = new ArrayList<String>();
        try {
            makeBlob(path, hashes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param path   - The path to the file to be backed up
     * @param hashes - An ArrayList used for detecting cycles
     * @return The hash of the data
     * @throws IOException
     * @throws FileNotFoundException
     * @throws IllegalStateException
     */
    private static String makeBlob(String path, ArrayList<String> hashes)
            throws IOException, FileNotFoundException, IllegalStateException {

        final String[] TYPEARRAY = new String[] { "blob", "tree" };

        File input = new File(path);
        if (!input.exists())
            throw new FileNotFoundException("File doesn't exist");

        // ignore restricted files
        if (!input.canRead()) {
            throw new AccessDeniedException("Can't read the file");
        }

        // ignore hidden files
        if (input.getName().length() > 0 && input.getName().charAt(0) == '.')
            return null;

        // generate hash
        String fileName;
        if (!input.isDirectory()) {
            fileName = generateFileHash(Files.readAllBytes(Paths.get(input.getPath())));
        } else {
            ArrayList<File> files = new ArrayList<File>();
            fileName = generateFileHash(treeToBytes(input, files));
        }

        // check for cycles
        if (hashes.contains(fileName)) {
            throw new IllegalStateException("Cycle detected");
        }
        hashes.add(fileName);

        // recursively call on directory contents
        if (input.isDirectory())
            for (File f : input.listFiles())
                makeBlob(f.getPath(), hashes);

        if (!input.isDirectory()) {
            // creates empty file in objects directory
            File copy = new File("git/objects/" + fileName);

            // copy the file
            if (!copy.exists())
                Files.copy(Path.of(input.getPath()), Path.of(copy.getPath()));

        } else {
            File copy = new File("git/objects/" + fileName);
            copy.createNewFile();
            FileOutputStream out = new FileOutputStream(copy);
            ArrayList<File> files = new ArrayList<File>();
            out.write(treeToBytes(input, files));
            out.close();
        }

        // inserts an entry into index file
        if (!inIndex(fileName)) {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("git/index", true)));
            pw.write(TYPEARRAY[input.isDirectory() ? 1 : 0] + " " + fileName + " " + input.getPath() + "\n");
            pw.close();
        }

        return fileName;
    }

    /**
     * @param hash - The hash of the file
     * @return True if the file is in the index
     * @throws IOException
     */
    private static boolean inIndex(String hash) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("git/index"));
        while (reader.ready()) {
            String line = reader.readLine();
            if (line.contains(hash)) {
                reader.close();
                return true;
            }
        }
        reader.close();
        return false;
    }

    /**
     * Commits the current staged changes.
     * 
     * @param author      - The author of the commit
     * @param description - The description of the commit
     * @return The hash of the latest commit
     * @throws IOException
     */
    @Override
    public String commit(String author, String message) {
        try {
            return commit(author, message, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Checks out a commit. Restores the working directory to the state from that
     * commit.
     * 
     * @param commitHash - the hash of the commit
     * 
     * @throws IOException
     */
    @Override
    public void checkout(String commitHash) {
        try {
            checkout(commitHash, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks out a commit. Restores the working directory to the state from that
     * commit.
     * 
     * @param commitHash - the hash of the commit
     * @param useless    - a useless variable used to overload the method; it
     *                   doesn't matter what you put here
     * @throws IOException
     */
    private void checkout(String commitHash, boolean useless) throws IOException {

        // clears the working directory before restoring commit
        clearWorkingDirectory();

        // restores the commit to the working directory
        restoreData(commitHash);
    }

    /**
     * Restores the data from a commit to the working directory.
     * 
     * @param commitHash - the hash of the commit to restore
     * @throws IOException
     */
    private static void restoreData(String commitHash) throws IOException {
        File commit = new File("git/objects/" + commitHash);
        String commitData = getStringFromFile(commit.getPath());
        int hashLocation = commitData.indexOf("tree") + 5;
        String treeHash = commitData.substring(hashLocation, hashLocation + 40);
        String treePath = "git/objects/" + treeHash;

        BufferedReader reader = new BufferedReader(new FileReader(treePath));
        while (reader.ready()) {
            String line = reader.readLine();
            if (line.contains("blob")) {
                restoreFile(line);
            } else if (line.contains("tree")) {
                restoreTree(line);
            }
        }
        reader.close();
    }

    /**
     * Restores a file to the working directory given its line from a tree file.
     * 
     * @param line - The line of information from the tree file
     * @throws IOException
     */
    private static void restoreFile(String line) throws IOException {
        String fileHash = line.substring(5, 45);
        String filePath = line.substring(46);
        Files.copy(Path.of("git/objects/" + fileHash), Path.of(filePath));
    }

    /**
     * Restores the tree to the working directory.
     * 
     * @param line - The line from the commit or tree that contains information
     *             about the tree to be restored
     * @throws IOException
     */
    private static void restoreTree(String line) throws IOException {
        String treeHash = line.substring(5, 45);
        String treePath = line.substring(46);
        File directory = new File(treePath);
        directory.mkdir();
        File treeFile = new File("git/objects/" + treeHash);

        BufferedReader reader = new BufferedReader(new FileReader(treeFile));
        while (reader.ready()) {
            String treeline = reader.readLine();
            if (treeline.contains("blob")) {
                restoreFile(treeline);
            } else if (treeline.contains("tree")) {
                restoreTree(treeline);
            }
        }
        reader.close();
    }

    /**
     * Clears the working directory of all files that do not end in .java and that
     * are not a part of the git repository data.
     */
    private static void clearWorkingDirectory() {
        File directory = new File("./");
        for (File file : directory.listFiles()) {
            if (file.isDirectory() && !file.getName().equals("git") && !file.getName().equals(".git")) {
                removeDirectory(file.getPath());
            } else if (file.isFile() && !file.getName().endsWith(".java")) {
                file.delete();
            }
        }
    }

    /**
     * Deletes a directory and all files within it.
     * 
     * @param directoryPath - the directory to delete
     */
    private static void removeDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        for (File file : directory.listFiles()) {
            if (file.isDirectory())
                removeDirectory(file.getPath());
            file.delete();
        }
        directory.delete();
    }
}