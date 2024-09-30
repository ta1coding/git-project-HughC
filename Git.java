import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Git {

    public final String[] TYPEARRAY = new String[] { "blob", "tree" };

    public static void main(String[] args) throws IOException {

    }

    // constructor
    public Git() throws IOException {
        initGitRepo();
    }

    // makes a Git Repo in the current folder
    private void initGitRepo() throws IOException {

        File gitDir = new File("git");

        File objectsDir = new File("git/objects");

        File indexFile = new File("git/index");

        // exist check
        if (gitDir.exists() && objectsDir.exists() && indexFile.exists()) {
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
        }
    }

    // recursively generate pre-hashed tree file to be used in generateFileName
    private byte[] treeToBytes(File input, ArrayList<File> files) throws IOException {
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
                String fHash = generateFileName(treeToBytes(f, files));
                sb.append("tree " + fHash + " " + fName + "\n");
            } else {
                String fHash = generateFileName(Files.readAllBytes(Paths.get(f.getPath())));
                sb.append("blob " + fHash + " " + fName + "\n");
            }
        }
        return sb.toString().getBytes();
    }

    // generates the hash string name according to SHA1
    private String generateFileName(byte[] input) throws IOException {
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

    public String makeBlob(String path) throws IOException, FileNotFoundException, IllegalStateException {
        ArrayList<String> hashes = new ArrayList<String>();
        return makeBlob(path, hashes);
    }

    public String makeBlob(String path, ArrayList<String> hashes)
            throws IOException, FileNotFoundException, IllegalStateException {

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
            fileName = generateFileName(Files.readAllBytes(Paths.get(input.getPath())));
        } else {
            ArrayList<File> files = new ArrayList<File>();
            fileName = generateFileName(treeToBytes(input, files));
        }

        // check for cycles
        if (hashes.contains(fileName)) {
            throw new IllegalStateException("Cycle detected");
        }
        hashes.add(fileName);

        // recursively call on directory contents
        if (input.isDirectory())
            for (String f : input.list())
                makeBlob(path + "/" + f, hashes);

        if (!input.isDirectory()) {
            // creates empty file in objects directory
            File copy = new File("git/objects/" + fileName);
            copy.createNewFile();

            // should make into bufferedInputStream and BufferedOutputStream later
            FileInputStream in = new FileInputStream(input);

            FileOutputStream out = new FileOutputStream(copy);

            // copies the contents of the file
            int n;
            while ((n = in.read()) != -1)
                out.write(n);

            in.close();
            out.close();
        } else {
            File copy = new File("git/objects/" + fileName);
            copy.createNewFile();
            FileOutputStream out = new FileOutputStream(copy);
            ArrayList<File> files = new ArrayList<File>();
            out.write(treeToBytes(input, files));
            out.close();
        }

        // inserts an entry into index file
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("git/index", true)));
        pw.write(TYPEARRAY[input.isDirectory() ? 1 : 0] + " " + fileName + " " + input.getPath() + "\n");
        pw.close();

        return fileName;
    }
}