package sam.manga.downloader;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sam.myutils.fileutils.FilesUtils;
import sam.properties.myconfig.MyConfig;
import sam.properties.myproperties.MyProperties;

public final class Utils {
    public static final MyProperties CONFIG_MAP = new MyProperties();
    public static final Path CURRENT_SESSION_FOLDER;
    public static final Path HALTED_IMAGE_DIR;
    public static final Path DOWNLOAD_DIR;
    public static final Path LOGS_FOLDER;
    public static final Path BACKUP_DIR;
    public static final Path MISSING_CHAPTERS_PATH = Paths.get(MyConfig.MISSING_CHAPTERS_PATH);
    public static final Path NEW_MANGAS_TSV_PATH = Paths.get(MyConfig.NEW_MANGAS_TSV_PATH);
    public static final Path MANGAROCK_INPUT_DB = Paths.get(MyConfig.MANGAROCK_INPUT_DB);
    public static final Path MANGAROCK_INPUT_FOLDER = Paths.get(MyConfig.MANGAROCK_INPUT_FOLDER);
    
    static {
        Path p = Paths.get("running");

        if(Files.exists(p)){
            FilesUtils.openFileLocationInExplorerNoError(new File("running"));
            JOptionPane.showMessageDialog(null, "Only One instance of downloader is allowed");
            System.exit(0);
        }
        else{
            try {
                Files.createFile(p);
                p.toFile().deleteOnExit();
            } catch (IOException e1) {
                showErrorJDialog("Failed to create file: running", null, e1);
                System.exit(0);
            }
        }

        try {
            CONFIG_MAP.load(Paths.get("config.properties"));
        } catch (IOException e1) {}

        CURRENT_SESSION_FOLDER = Paths.get(CONFIG_MAP.containsKey("session_1") ? CONFIG_MAP.get("session_1") : "current_session");
        HALTED_IMAGE_DIR = CURRENT_SESSION_FOLDER.resolve("mangafox_halted");
        DOWNLOAD_DIR = CURRENT_SESSION_FOLDER.resolve("mangafox_downloaded");
        LOGS_FOLDER = CURRENT_SESSION_FOLDER.resolve("logs");
        BACKUP_DIR = CURRENT_SESSION_FOLDER.resolve("backups");

        if(Files.notExists(CURRENT_SESSION_FOLDER))
            CONFIG_MAP.clear();

        if(!CONFIG_MAP.containsKey("session_1"))
            CONFIG_MAP.put("session_1","current_session");

        try {
            Files.createDirectories(HALTED_IMAGE_DIR);
            Files.createDirectories(DOWNLOAD_DIR);
        } catch (IOException e1) {
            showErrorJDialog("Error creating dir: \n\tHALTED_IMAGE_DIR: "+HALTED_IMAGE_DIR+"\n\tDOWNLOAD_DIR: "+DOWNLOAD_DIR, "App will not start", e1);            
            System.exit(0);
        }
    }
    
    private static void showErrorJDialog(String msg, String title, Exception e1) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println(msg);
        pw.println();
        e1.printStackTrace(pw);

        JTextArea area = new JTextArea(sw.toString());
        area.setEditable(false);

        JDialog dialog = new JDialog(null, title, ModalityType.APPLICATION_MODAL);
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(area, new JScrollPane(area));
        dialog.pack();

        dialog.setVisible(true);
    }
    
    private Utils() {}

}
