import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Git{
    public static void main(String[] args) throws IOException {

    }

    //constructor
    public Git () throws IOException{
        initGitRepo();
    }

    //makes a Git Repo in the current folder 
    private void initGitRepo() throws IOException{

        File gitDir = new File("git");

        File objectsDir = new File("git/objects");

        File indexFile = new File("git/index");

        //exist check
        if (gitDir.exists() && objectsDir.exists() && indexFile.exists()){
            System.out.println("Git Repository already exists");
        }
        else{
            if (!gitDir.exists()){
                gitDir.mkdir();
            }
            if (!objectsDir.exists()){
                objectsDir.mkdir();
            }
            if (!indexFile.exists()){
                indexFile.createNewFile();
            }
        }
    }
    
    //generates the hash string name according to SHA1
    private String generateFileName(File input) throws IOException{
                try {
            
            // getInstance() method is called with algorithm SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(Files.readAllBytes(Paths.get(input.getPath())));

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

    //
    public void makeBlob(File input) throws IOException{
        String fileName = generateFileName(input);

        //creates empty file in objects directory
        File copy = new File("git/objects/" + fileName);
        copy.createNewFile();



        //should make into bufferedInputStream and BufferedOutputStream later
        FileInputStream in = new FileInputStream(input);

        FileOutputStream out = new FileOutputStream(copy);

        //copies the contents of the file
        int n;
        while ((n = in.read()) != -1){
            out.write(n);
        }
        in.close();
        out.close();



        //inserts an entry into index file
        FileWriter fw = new FileWriter("git/index");
        fw.write(fileName + " " + input.getName());
        fw.close();
    }
}