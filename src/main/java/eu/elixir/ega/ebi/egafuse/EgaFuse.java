/*
 * Copyright 2016 ELIXIR EGA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.elixir.ega.ebi.egafuse;

import com.google.api.client.auth.oauth2.PasswordTokenRequest;
import com.google.api.client.auth.oauth2.ClientCredentialsTokenRequest;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiDirectory;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiFile;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiPath;
import eu.elixir.ega.ebi.egafuse.filesystems.internal.EgaDatasetDirectory;
import eu.elixir.ega.ebi.egafuse.filesystems.internal.EgaNodeDirectory;
import eu.elixir.ega.ebi.egafuse.filesystems.internal.EgaNodeFile;
import eu.elixir.ega.ebi.egafuse.filesystems.internal.EgaRemoteFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import jnr.ffi.Pointer;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.identityconnectors.common.security.GuardedString;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;

import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;

/**
 *
 * @author asenf
 */
public class EgaFuse extends FuseStubFS {
    private static boolean grid = false;
    private static String gridOrg = null;
    private static String basicCode = null;
    
    // Specified by command line parameters
    private static GuardedString username; // Optional, if token is specified
    private static GuardedString password; // Optional, if token is specified
    private static String mountDir="/tmp/mnt"; // Required: mount directory
    private static String accessToken; // If missing: obtain; requires aaiUrl
    private static String refreshToken; // Test
    private static String baseUrl = "https://ega.ebi.ac.uk:8051/elixir/data"; // API URL. Required 
            // for CSC: "http://data.epouta.lega.csc.fi:8686/elixir/data"; -- Specify via option upon start
    private static String cegaUrl = "https://ega.ebi.ac.uk:8051/elixir/central"; // API URL. Required
    private static Options options = new Options();

    // *************************************************************************
    protected static HashMap<String,String> aaiConfig = null;    
    // *************************************************************************
    
    /** Directory to store user credentials. */
    private static final java.io.File DATA_STORE_DIR =
        new java.io.File(System.getProperty("user.home"), ".store/sample");
    
    /**
     * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
     * globally shared instance across your application.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** OAuth 2 scope. */
    private static String SCOPE = "openid";

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    static final JsonFactory JSON_FACTORY = new JacksonFactory();

    // EGA AAI
    private static final String TOKEN_SERVER_URL = "https://ega.ebi.ac.uk:8443/ega-openid-connect-server/token";
    private static final String AUTHORIZATION_SERVER_URL = "https://ega.ebi.ac.uk:8443/ega-openid-connect-server/authorize";

    // FUSE Mode: User Mapping EGA-User -- Linux User (default is: identical)
    private static HashMap<String,String> userMapping = null;
    private static HashMap<String,String> orgMapping = null;

    // FUSE Mode: Obtain Local System User IDs for Permissions Mapping
    private static HashMap<String,Integer> uidMapping = null;
    private static HashMap<String,Integer> gidMapping = null;
    
    /*
     * Instantiate the FUSE Layer
     * - Build Virtual Directory Structure by interactine with API
     * 
     * Directories are empty until they are accessed ('ls')
     * Upon first access there is an API call to popuate the directories
     */
    private EgaApiDirectory rootDirectory = new EgaApiDirectory("", null);    
    public EgaFuse(String token) { // Only valid with Access Token!
        if (SCOPE.equalsIgnoreCase("openid")) {            
            rootDirectory.add(new EgaDatasetDirectory("Datasets", rootDirectory));
        } 
    }
    public EgaFuse(boolean basicgrid) { // Only valid with Access Token!
        if (basicgrid) {
            getOrgsNodes();            
        }
    }
    
    /* https://ega.ebi.ac.uk:8051/elixir/
     * Start of the FUSE Layer
     * 
     * 2 Options: 
     *    - Start with EGA Username/Password
     *    - Start with valid EGA Access Token
     * 
     * java -jar EgaFuse.jar -u {username} -p {password} -m {mount dir}
     * java -jar EgaFuse.jar -fu {file containing username/password} -m {mount dir}
     * java -jar EgaFuse.jar -t {accesstoken} -m {mount dir}
     * java -jar EgaFuse.jar -ft {file containing accesstoken} -m {mount dir}
     * 
     * java -jar EgaFuse.jar -g {org} -m {mount dir} [-gf {mapping file path}]
     * 
     */
    public static void main(String[] args) {
        
        // Deal with non-CA SSL Certificate [test]
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();
        
        // Command Line Processing
        setOptions();
        parse(args);
        
        // Start the FUSE Layer
        if (accessToken!=null && accessToken.length()>0 && !grid) {
            EgaFuse the_fuse = new EgaFuse(accessToken);
            try {
                String[] args_ = new String[]{"-o", "allow_other"}; // Allow non-root access
                the_fuse.mount(Paths.get(mountDir), true, true, args_);
            } finally {
                the_fuse.umount();
            }
        } else if (grid) {
            EgaFuse the_fuse = new EgaFuse(grid);
            try {
                String[] args_ = new String[]{"-o", "allow_other"}; // Allow non-root access
                the_fuse.mount(Paths.get(mountDir), true, true, args_);
            } finally {
                the_fuse.umount();
            }
        }
    }

    /*
     * Command Line processing -- Setting Options, Parsing 
     */
    private static void setOptions() {
        
        options.addOption("f", "configfile", true, "Configuration file path.");
        options.addOption("u", "username", true, "Specify Username.");
        options.addOption("p", "password", true, "Specify Password.");
        options.addOption("m", "mount", true, "Specify Mount Directory.");
        options.addOption("t", "token", true, "Specify Access Token.");
        options.addOption("rt", "refresh_token", true, "Specify Refresh Token.");
        options.addOption("fu", "fileuser", true, "Specify File containing Username/Password");
        options.addOption("ft", "filetoken", true, "Specify File containing Access Token");
        options.addOption("g", "gridfuse", true, "Starts in GridFTP mode; required: orgaization");
        options.addOption("gf", "gridfile", true, "Account Mapping File");
        options.addOption("url", "baseurl", true, "Alternate FUSE Base URL");
        options.addOption("h", "help", false, "Display Help.");
 
    }
    
    private static void parse(String[] args) {
                
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {       // HELP
                help();
                System.exit(0);
            }
            if (cmd.hasOption("url")) {     // Provide a custom FUSE URL
                baseUrl = cmd.getOptionValue("url");
            }
            if (cmd.hasOption("f")) {       // USERNAME specified
                String path = cmd.getOptionValue("f");
                try {
                    getConfig(path);
                } catch (FileNotFoundException ex) {
                    System.out.println("Config File error: " + ex.getMessage());
                    System.exit(7);
                }
            }
            if (cmd.hasOption("u")) {       // USERNAME specified
                username = new GuardedString(cmd.getOptionValue("u").toCharArray());
            }
            if (cmd.hasOption("p")) {       // PASSWORD specified
                password = new GuardedString(cmd.getOptionValue("p").toCharArray());
            }
            if (cmd.hasOption("m")) {       // MOUNT DIRECTORY specified
                mountDir = cmd.getOptionValue("m");
            }
            File mntTest = new File(mountDir);
            if (!mntTest.exists() && !mntTest.isDirectory()) {
                System.out.println(mntTest + " can not be used as mount point.");
                System.out.println("Ensure that the directory path exists and is empty!");
                System.exit(1);
            }
            if (cmd.hasOption("t")) {       // ACCESS TOKEN specified
                accessToken = cmd.getOptionValue("t");
            }
            if (cmd.hasOption("rt")) {       // REFRESH TOKEN specified
                refreshToken = cmd.getOptionValue("rt");
            }
            try {
                if (cmd.hasOption("fu")) {  // USERNAME/PASSWORD FILE soecified
                    readUsernamePasswordfromFile(cmd.getOptionValue("fu"));
                }
                if (cmd.hasOption("ft")) {  // ACCESS TOKEN FILE specified
                    readAccessTokenfromFile(cmd.getOptionValue("ft"));
                }
            } catch (FileNotFoundException ex) {
                System.out.println("File Not Found Error: " + ex.getMessage());
                System.exit(1);
            }
            
            if (cmd.hasOption("g")) {       // Non-User Mode
                grid = true;
                gridOrg = cmd.getOptionValue("g");
System.out.println("Grid Org: " + gridOrg);
                try {
                    if (cmd.hasOption("gf")) {  // Mapping File Specified (only if -g)
                        getUserMapping(cmd.getOptionValue("gf"));
                        getUserSystemIDs();
                    }
                } catch (Exception ex) {
                    System.out.println("Linux/EGA User Mapping Error (does the Linux user exist?): " + ex.getMessage());
                    System.exit(7);
                }
                
//                basicCode = URLEncoder.encode(cmd.getOptionValue("u") + ":" + cmd.getOptionValue("p"));
//System.out.println("basicCode: " + basicCode);
                
                /*  Replaced by Basic Auth - use username & password to get Basic Auth header
                try { // always required
                    TokenResponse token = authorizeApp();
                    accessToken = token.getAccessToken();
                    refreshToken = token.getRefreshToken();
                } catch (Exception ex) {
                    System.out.println("Grid Mode/Authentication Error: " + ex.getMessage());
                    System.exit(7);
                }
                */                
            }
            

            if (username!=null && password!=null && accessToken==null) { // Get ACCESS TOKEN
                try {
                    TokenResponse token = authorize();
                    accessToken = token.getAccessToken();
                    refreshToken = token.getRefreshToken();
                } catch (Exception ex) {
                    System.out.println("Authentication Error: " + ex.getMessage());
                    System.exit(4);
                }
            }
            
        } catch (ParseException e) {
            help();
            System.exit(3);
        }
        
    }
    
    private static void help() {
        HelpFormatter formater = new HelpFormatter();
        formater.printHelp("Main", options);
        System.exit(0);
    }
    
    private static void readUsernamePasswordfromFile(String filepath) throws FileNotFoundException {
        
        FileInputStream fis = new FileInputStream(filepath);
        Scanner scanner = new Scanner(fis);
      
        if (scanner.hasNextLine()) {
            username = new GuardedString(scanner.nextLine().toCharArray());
        } else {
            System.out.println("Username not Specified in File " + filepath);
            System.exit(1);
        }
        if (scanner.hasNextLine()) {
            password = new GuardedString(scanner.nextLine().toCharArray());
        } else {
            System.out.println("Password not Specified in File " + filepath);
            System.exit(2);
        }
      
        scanner.close();

    }
    
    private static void readAccessTokenfromFile(String filepath) throws FileNotFoundException {
        
        FileInputStream fis = new FileInputStream(filepath);
        Scanner scanner = new Scanner(fis);
      
        if (scanner.hasNextLine()) {
            accessToken = scanner.nextLine();
            if (scanner.hasNextLine()) {
                refreshToken = scanner.nextLine();
            }
        } else {
            System.out.println("Access Token not Specified in File " + filepath);
            System.exit(1);
        }
      
        scanner.close();
        
    }

    /*
     * Code for OAuth2 Authentication [doubles as Refresh in certain cases]
     */

    private static TokenResponse authorize() throws Exception {
        
        final StringBuilder clearUser = new StringBuilder();
        username.access(new GuardedString.Accessor() {
          @Override
          public void access(final char[] clearChars) {
            clearUser.append(clearChars);
          }
        });
        final StringBuilder clearPass = new StringBuilder();
        password.access(new GuardedString.Accessor() {
          @Override
          public void access(final char[] clearChars) {
            clearPass.append(clearChars);
          }
        });
        
        TokenResponse response = 
            new PasswordTokenRequest(HTTP_TRANSPORT, 
                                   JSON_FACTORY, 
                                   new GenericUrl(TOKEN_SERVER_URL), 
                                   clearUser.toString(), 
                                   clearPass.toString())
                .setGrantType(aaiConfig.get("userGrant").trim())
                .setClientAuthentication( 
                    new BasicAuthentication(aaiConfig.get("userId").trim(), 
                            aaiConfig.get("userSecret").trim())
            ).execute(); 
        
        return response;
    }
    public static void refreshAuthorize() throws Exception {
        
        TokenResponse response = 
            new RefreshTokenRequest(HTTP_TRANSPORT, 
                                   JSON_FACTORY, 
                                   new GenericUrl(TOKEN_SERVER_URL), 
                                   refreshToken)
                .setGrantType("refresh_token")
                .setClientAuthentication( 
                    new BasicAuthentication(aaiConfig.get("userId").trim(), 
                            aaiConfig.get("userSecret").trim())
            ).execute(); 
        
            accessToken = response.getAccessToken();
            refreshToken = response.getRefreshToken();

        return;
    }
/*
    private static TokenResponse authorizeApp() throws Exception {
        Collection<String> scopes = new ArrayList<>();
        scopes.add(aaiConfig.get("fuseScope").trim());

        TokenResponse response = 
            new ClientCredentialsTokenRequest(HTTP_TRANSPORT, 
                                   JSON_FACTORY, 
                                   new GenericUrl(TOKEN_SERVER_URL))
                .setGrantType(aaiConfig.get("fuseGrant").trim())
                .setScopes(scopes)
                .setClientAuthentication( 
                    new BasicAuthentication(aaiConfig.get("fuseId").trim(),
                                            aaiConfig.get("fuseSecret").trim())
            ).execute(); 
        
        SCOPE = "fuse";
        
        return response;
    }
*/    
    public static String getBasicCode() {
        return aaiConfig.get("user").toString();
    }

    public static String getToken() {
        return accessToken;
    }
    
    public static String getApiUrl() {
        return baseUrl;
    }
    
    public static String getCentralUrl() {
        return cegaUrl;
    }
    
    /*
     * Should be Replaced with specified Org
     */
    /*
    private void getOrgsNodes() {
        if (userMapping==null) { // If no mapping file is specified, use identity
            userMapping = new HashMap<>();
            orgMapping = new HashMap();
        }
        
        OkHttpClient client = SSLUtilities.getUnsafeOkHttpClient();
        String basicUser = aaiConfig.get("user").toString();
        
        // List all Datasets
        Request datasetRequest = new Request.Builder()
            .url(cegaUrl + "/app/orgs")
            .addHeader("Authorization", "Basic " + basicUser)
            .build();
        Moshi MOSHI = new Moshi.Builder().build();
        JsonAdapter<List<String>> STRING_JSON_ADAPTER = 
                MOSHI.adapter(Types.newParameterizedType(List.class, String.class));
        
        try {
            // Execute the request and retrieve the response.
            Response response = null;
            int tryCount = 9;
            while (tryCount-->0 && (response == null || !response.isSuccessful())) {
                try {
                    response = client.newCall(datasetRequest).execute();
                } catch (Exception ex) {}
            }
            ResponseBody body = response.body();
            List<String> orgs = STRING_JSON_ADAPTER.fromJson(body.source());
            body.close();
            System.out.println(orgs.size() + " orgs with registered public keys found.");
            
            for (String org:orgs) {
                if (!orgMapping.containsKey(org)) { // Default: identity
                    userMapping.put(org, org);
                    orgMapping.put(org, org);
                } 
                
                // Map Directory Org to Linux User Name
                String userName = orgMapping.get(org);
                
                EgaNodeDirectory egaNodeDirectory = new EgaNodeDirectory(userName, rootDirectory);
                rootDirectory.add(egaNodeDirectory);
            }
        } catch (IOException ex) {
            System.out.println("Error getting Datasets: " + ex.toString());
        }        
    }
    */
    private void getOrgsNodes() {
        String plainOrg = "publicgpg_" + gridOrg;
        EgaNodeDirectory egaNodeDirectory = new EgaNodeDirectory(plainOrg, rootDirectory);
        rootDirectory.add(egaNodeDirectory);
    }
    
    private static void getConfig(String filepath) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(filepath);
        Scanner scanner = new Scanner(fis);
        aaiConfig = new HashMap<>();
      
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            StringTokenizer st = new StringTokenizer(line, ":");
            String key = st.nextToken(":");
            String value = st.nextToken(":");
            aaiConfig.put(key, value);
        }
      
        scanner.close();        
    }
    
    /*
     * May be deprecated
     */
    private static void getUserMapping(String filepath) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(filepath);
        Scanner scanner = new Scanner(fis);
        userMapping = new HashMap<>();
        orgMapping = new HashMap<>();
      
        while (scanner.hasNext()) {  // expect 'eganame':'linuxname'
            String line = scanner.nextLine();
            StringTokenizer st = new StringTokenizer(line, ":");
            String eganame = st.nextToken(":");
            String linuxuser = st.nextToken(":");
            orgMapping.put(eganame, linuxuser); // PublicGPG_ --> user
            userMapping.put(linuxuser, eganame); // user --> PublicGPG_
        }
      
        scanner.close();        
    }
    
    private static void getUserSystemIDs() throws FileNotFoundException {
        Set<String> keySet = userMapping.keySet();
        Iterator<String> iter = keySet.iterator();

        uidMapping = new HashMap<>();
        gidMapping = new HashMap<>();
        
        Process p;
        try {
            while (iter.hasNext()) {
                String linuxuser = iter.next();

                p = Runtime.getRuntime().exec("id -u " + linuxuser);
                p.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();
                uidMapping.put(linuxuser, Integer.parseInt(line));
                
                p = Runtime.getRuntime().exec("id -g " + linuxuser);
                p.waitFor();
                reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                line = reader.readLine();
                gidMapping.put(linuxuser, Integer.parseInt(line));                
            }
        } catch (IOException | InterruptedException ex) {
            System.out.println("Error Obtaining UID and GID information for specified user(s)");
            System.exit(7);
        }
    }
    
    public static String getOrg(String userName) {
        return (userMapping!=null&&userMapping.containsKey(userName))?
                    userMapping.get(userName):
                    userName;
    }
    public static int getUid(String userName) {
        return (uidMapping!=null&&uidMapping.containsKey(userName))?
                    uidMapping.get(userName):
                    0;
    }
    public static int getGid(String userName) {
        return (gidMapping!=null&&gidMapping.containsKey(userName))?
                    gidMapping.get(userName):
                    0;
    }
    
    public EgaApiPath getRootDir() {
        return rootDirectory;
    }
    
    // *************************************************************************
    // *************************************************************************
    // *************************************************************************
    // File System Operations
    // *************************************************************************

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
        EgaApiPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof EgaApiDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        filter.apply(buf, ".", null, 0);
        filter.apply(buf, "..", null, 0);
        ((EgaApiDirectory) p).read(buf, filter);
        return 0;
    }
    
    private EgaApiPath getParentPath(String path) {
        return rootDirectory.find(path.substring(0, path.lastIndexOf("/")));
    }

    private EgaApiPath getPath(String path) {
        return rootDirectory.find(path);
    }
    
    @Override
    public int getattr(String path, FileStat stat) {
        EgaApiPath p = getPath(path);
        if (p != null) {
            p.getattr(stat);
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    private String getLastComponent(String path) {
        while (path.substring(path.length() - 1).equals("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            return "";
        }
        return path.substring(path.lastIndexOf("/") + 1);
    }
    
    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        EgaApiPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof EgaApiFile)) {
            return -ErrorCodes.EISDIR();
        }
        
        if (p instanceof EgaRemoteFile) {
            return ((EgaRemoteFile) p).read(buf, size, offset);
        } else {
            return ((EgaNodeFile) p).read(buf, size, offset);
        }
    }

    @Override
    public int unlink(String path) {
        EgaApiPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        p.delete();
        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        EgaApiPath p = getPath(path);
        
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof EgaApiFile)) {
            return -ErrorCodes.EISDIR();
        } else {
            int open = ( (EgaApiFile)p ).open();
        }
    
        return 0;
    }
}
