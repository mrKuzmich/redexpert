package org.executequery;

import org.apache.commons.lang.StringUtils;
import org.executequery.http.JSONAPI;
import org.executequery.http.ReddatabaseAPI;
import org.executequery.log.Log;
import org.executequery.util.ApplicationProperties;
import org.executequery.util.UserProperties;
import org.json.JSONObject;
import org.underworldlabs.util.MiscUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by vasiliy on 16.01.17.
 */
public class UpdateLoader extends JFrame {

    private Thread worker;
    private String binaryZipUrl;
    private boolean releaseHub;

    public String getRepo() {
        return repo;
    }

    private static String repo;
    private String version = null;
    private String downloadLink;

    private JTextArea outText;
    private JButton cancelButton;
    private JButton restartButton;
    private JScrollPane scrollPane;
    private JPanel panel1;
    private JPanel panel2;
    private String repoArg;

    public void setRepoArg(String repoArg) {
        this.repoArg = repoArg;
    }

    public void setExternalArg(String externalArg) {
        this.externalArg = externalArg;
    }

    private String externalArg;

    private String root = "update/";

    public UpdateLoader(String repository) {
        initComponents();
        repo = repository;
    }

    private String getLastVersion(String repo) {
        StringBuilder buffer = new StringBuilder();
        try {
            URL myUrl = new URL(repo);
            URLConnection myUrlCon = myUrl.openConnection();
            InputStream input = myUrlCon.getInputStream();
            int c;
            while (((c = input.read()) != -1)) {
                buffer.append((char) c);
            }
            input.close();
        } catch (Exception e) {
            Log.error("Cannot download update from repository. " +
                    "Please, check repository url or try update later.");
            return null;
        }
        String s = buffer.toString();
        String[] ss = s.split("\n");
        String res = "0.0";
        for (int i = 0; i < ss.length; i++) {
            Pattern pattern;
            pattern = Pattern.compile("(<a href=\")([0-9]+[\\.][0-9]+.+)(/\">)");
            Matcher m = pattern.matcher(ss[i]);
            if (m.find()) {
                String r = m.group(2);
                try {
                    ApplicationVersion temp = new ApplicationVersion(r);
                    if (temp.isNewerThan(res))
                        res = r;
                } catch (Exception e) {
                    Log.debug("Big version:" + r, e);
                }

            }
        }
        if (!Objects.equals(res, "0.0"))
            return res;
        else
            return null;
    }

    public void setReleaseHub(boolean releaseHub) {
        this.releaseHub = releaseHub;
    }

    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());

        panel2 = new JPanel();
        panel2.setLayout(new FlowLayout());

        outText = new JTextArea();
        outText.setFont(UIManager.getDefaults().getFont("Label.font"));
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(outText);

        restartButton = new JButton("Restart now");
        restartButton.setEnabled(false);
        restartButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                launch();
            }
        });
        panel2.add(restartButton);

        cancelButton = new JButton("Cancel Update");
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelUpdate();
            }
        });
        panel2.add(cancelButton);
        panel1.add(scrollPane, BorderLayout.CENTER);
        panel1.add(panel2, BorderLayout.SOUTH);

        add(panel1);
        pack();
        this.setSize(500, 400);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
    }

    private void cancelUpdate() {

        this.dispose();
    }

    public static void main(String[] args) {
        applySystemProperties();
        UpdateLoader updateLoader = new UpdateLoader(repo);
        for (String arg :
                args) {
            if (arg.equalsIgnoreCase("usereleasehub")) {
                updateLoader.setReleaseHub(true);
            } else if (arg.contains("version")) {
                int i = arg.indexOf('=');
                String ver = arg.substring(i + 1);
                updateLoader.setVersion(ver);
            } else if (arg.contains("-repo")) {
                updateLoader.setRepoArg(arg);
            } else if (arg.contains("externalProcessName")) {
                int i = arg.indexOf('=');
                String external = arg.substring(i + 1);
                updateLoader.setExternalArg(external);
            }

        }
        updateLoader.setVisible(true);
        updateLoader.update();
    }
    private void download() {
        worker = new Thread(
                () -> {
                    try {
                        String parent = new File(ExecuteQuery.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
                        parent += "/";
                        File aNew = new File(parent);
                        root = parent + "/update/";
                        downloadFile(downloadLink);
                        unzip();
                        aNew.mkdir();
                        copyFiles(new File(root), aNew.getAbsolutePath());
                        cleanup();
                        restartButton.setEnabled(true);
                        outText.setText(outText.getText() + "\nUpdate Finished!");
                        cancelButton.setText("Restart later");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "An error occurred while preforming update!");
                    }
                });
        worker.start();
    }

    private void launch() {
        String repo = repoArg;
        String externalProcessName = externalArg;
        String[] run;
        if (externalProcessName != null && !externalProcessName.isEmpty())
            run = new String[]{externalProcessName, repo};
        else {
            File file = new File("RedExpert.jar");
            if (!file.exists())
                file = new File("../RedExpert.jar");
            run = new String[]{"java", "-jar", file.getAbsolutePath(), repo};
        }
        try {
            Runtime.getRuntime().exec(run);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }

    private void cleanup() {
        outText.setText(outText.getText() + "\nPreforming clean up...");
        File f = new File("update.zip");
        f.delete();
        remove(new File(root));
        try {
            Files.delete(Paths.get(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void remove(File f) {
        File[] files = f.listFiles();
        if (files != null) {
            for (File ff : files) {
                if (ff.isDirectory()) {
                    remove(ff);
                    ff.delete();
                } else {
                    ff.delete();
                }
            }
        }
    }

    private void copyFiles(File f, String dir) {
        File[] files = f.listFiles();
        if (files != null) {
            for (File ff : files) {
                if (ff.isDirectory()) {
                    new File(dir + "/" + ff.getName()).mkdir();
                    copyFiles(ff, dir + "/" + ff.getName());
                } else {
                    try {
                        copy(ff.getAbsolutePath(), dir + "/" + ff.getName());
                    } catch (IOException e) {
                        outText.setText(outText.getText() + "\n Copying error. " +
                                e.getMessage());
                    }
                }

            }
        }
    }

    public void copy(String srFile, String dtFile) throws IOException {

        File f1 = new File(srFile);
        File f2 = new File(dtFile);

        InputStream in = new FileInputStream(f1);

        OutputStream out = new FileOutputStream(f2);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void unzip() {
        int BUFFER = 2048;
        BufferedOutputStream dest = null;
        BufferedInputStream is;
        ZipEntry entry;
        try {
            ZipFile zipfile = new ZipFile("update.zip");
            Enumeration e = zipfile.entries();
            (new File(root)).mkdir();
            while (e.hasMoreElements()) {
                entry = (ZipEntry) e.nextElement();
                outText.setText(outText.getText() + "\nExtracting: " + entry);
                if (entry.isDirectory())
                    (new File(root + entry.getName())).mkdir();
                else {
                    (new File(root + entry.getName())).createNewFile();
                    is = new BufferedInputStream
                            (zipfile.getInputStream(entry));
                    int count;
                    byte[] data = new byte[BUFFER];
                    try {
                        FileOutputStream fos = new
                                FileOutputStream(root + entry.getName());
                        dest = new
                                BufferedOutputStream(fos, BUFFER);
                        while ((count = is.read(data, 0, BUFFER))
                                != -1) {
                            dest.write(data, 0, count);
                        }
                    } catch (FileNotFoundException ex) {
                        outText.setText(outText.getText() + "\nExtracting " + entry + " error. " +
                                ex.getMessage());
                    } catch (IOException ex) {
                        outText.setText(outText.getText() + "\nExtracting " + entry + "error." +
                                ex.getMessage());
                    }
                    if (dest != null) {
                        dest.flush();
                        dest.close();
                    }
                    is.close();
                }
            }
            zipfile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void applySystemProperties() {

        String encoding = stringApplicationProperty("system.file.encoding");
        if (StringUtils.isNotBlank(encoding)) {

            System.setProperty("file.encoding", encoding);
        }

        String settingDirName = stringApplicationProperty("eq.user.home.dir");
        System.setProperty("executequery.user.home.dir", settingDirName);
        ApplicationContext.getInstance().setUserSettingsDirectoryName(settingDirName);

        String build = stringApplicationProperty("eq.build");
        System.setProperty("executequery.build", build);
        ApplicationContext.getInstance().setBuild(build);
    }

    private static String stringApplicationProperty(String key) {

        return applicationProperties().getProperty(key);
    }

    private static ApplicationProperties applicationProperties() {

        return ApplicationProperties.getInstance();
    }

    public boolean isNeedUpdate() {
        version = getLastVersion(repo);
        String localVersion = System.getProperty("executequery.minor.version");

        if (version != null && localVersion != null) {
            int newVersion = Integer.valueOf(version.replaceAll("\\.", ""));
            int currentVersion = Integer.valueOf(localVersion.replaceAll("\\.", ""));
            return (newVersion > currentVersion);
        }
        return false;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setBinaryZipUrl(String binaryZip) {
        this.binaryZipUrl = binaryZip;
    }

    void update() {
        this.setTitle("Updating");
        if (releaseHub) {
            outText.setText("Contacting Download Server...");
            try {

                JSONObject obj = JSONAPI.getJsonObjectFromArray(JSONAPI.getJsonArray(
                        "http://builds.red-soft.biz/api/artifacts/by_build/?project=red_expert&version=" + version),
                        "artifact_id",
                        "red_expert:red_expert:" + version + ":zip:bin");
                downloadLink = "http://builds.red-soft.biz/" + obj.getString("file");
                download();
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        } else {
            outText.setText("Contacting Download Server...");
            if (!MiscUtils.isNull(repo)) {
                this.downloadLink = repo + "/" + version + "/red_expert-" + version + "-bin.zip";
                download();
            } else {
                try {
                    //изменить эту строку в соответствии с форматом имени файла на сайте
                    String filename = UserProperties.getInstance().getStringProperty("reddatabase.filename") + version + ".zip";
                    Map<String, String> heads = ReddatabaseAPI.getHeadersWithToken();
                    if (heads != null) {
                        String url = JSONAPI.getJsonObjectFromArray(JSONAPI.getJsonArray(UserProperties.getInstance().getStringProperty("reddatabase.get-files.url") + version,
                                heads), "filename", filename).getString("url");
                        downloadLink = JSONAPI.getJsonPropertyFromUrl(url + "genlink/", "link", heads);
                        download();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void downloadFile(String link) throws IOException {
        URL url = new URL(link);
        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();
        long max = conn.getContentLength();
        outText.setText(outText.getText() + "\n" + "Downloading file...\nUpdate Size(compressed): " + max + " Bytes");
        BufferedOutputStream fOut = new BufferedOutputStream(new FileOutputStream(new File("update.zip")));
        byte[] buffer = new byte[32 * 1024];
        int bytesRead;
        int in = 0;
        while ((bytesRead = is.read(buffer)) != -1) {
            in += bytesRead;
            fOut.write(buffer, 0, bytesRead);
        }
        fOut.flush();
        fOut.close();
        is.close();
        outText.setText(outText.getText() + "\nDownload Complete!");

    }

}
