import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class SQLServerMain {
    // Connect to your database.
    // Replace server name, username, and password with your credentials
    public static void main(String[] args) {

        String[] login = getLoginInfo();
        String username = login[0];
        String password = login[1];

        DatabaseConnection db = new DatabaseConnection(username, password);
        DatabaseConsole.runConsole(db);

        System.out.println("Exiting...");
    }

    private static String[] getLoginInfo() {
        Properties prop = new Properties();
        String fileName = "auth.cfg";

        try {
            FileInputStream configFile = new FileInputStream(fileName);
            prop.load(configFile);
            configFile.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Could not find config file.");
            System.exit(1);
        } catch (IOException ex) {
            System.out.println("Error reading config file.");
            System.exit(1);
        }

        String username = (prop.getProperty("username"));
        String password = (prop.getProperty("password"));

        if (username == null || password == null){
            System.out.println("Username or password not provided.");
            System.exit(1);
        }

        String[] login = {username, password};
        return login;
    }
}
