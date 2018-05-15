package sam.manga.downloader.extra;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import javafx.application.HostServices;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sam.fileutils.FilesUtils;
import sam.fx.alert.FxAlert;
import sam.manga.downloader.scrapper.Scrapper;
import sam.weak.LazyAndWeak;

public final class Utils {
    private Utils() {}
    private final static Logger LOGGER = Logger.getGlobal();

    public static final Path SESSION_DIR;
    public static final Path HALTED_IMAGE_DIR;
    public static final Path DOWNLOAD_DIR;
    public static final Path LOGS_DIR;

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
                showError("Failed to create file: running", null, e1, true);
                System.exit(0);
            }
        }
        SESSION_DIR = Paths.get("downloads/"+Scrapper.URL_COLUMN);
        HALTED_IMAGE_DIR = SESSION_DIR.resolve("halted");
        DOWNLOAD_DIR = SESSION_DIR.resolve("downloaded");
        LOGS_DIR = SESSION_DIR.resolve("logs");

        try {
            Files.createDirectories(HALTED_IMAGE_DIR);
            Files.createDirectories(DOWNLOAD_DIR);
        } catch (IOException e1) {
            showError(String.format("Error creating dir: \n\tHALTED_IMAGE_DIR: %s\n\tDOWNLOAD_DIR: %s",HALTED_IMAGE_DIR, DOWNLOAD_DIR), "App will not start", e1, true);            
            System.exit(0);
        }
    }

    public static Path path(String uri) { return Paths.get(uri); }

    private static Stage stage;
    private static HostServices hostServices;

    public static HostServices hostService() { return hostServices; }
    public static Stage stage(){ return stage; }
    public static void open(String uri){ hostServices.showDocument(uri); }
    public static void open(Path p){ open(p.toUri().toString()); }

    public static  void init(Stage stage, HostServices hostServices) {
        Utils.stage = stage;
        Utils.hostServices = hostServices;
    }

    public static void stop() {
        try {
            Files.deleteIfExists(LOGS_DIR);
            Files.deleteIfExists(DOWNLOAD_DIR);
            Files.deleteIfExists(HALTED_IMAGE_DIR);
        } catch (IOException e1) {}
    }

    public static void showError(String msg, String title, Exception e1, boolean block) {
        if(stage != null) {
            Alert a = FxAlert.alertBuilder(AlertType.ERROR)
                    .modality(block ? Modality.APPLICATION_MODAL : Modality.WINDOW_MODAL)
                    .content(msg)
                    .header(title)
                    .exception(e1)
                    .build();

            if(block)
                a.showAndWait();
            else
                a.show();
            return;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println(msg);
        pw.println();
        e1.printStackTrace(pw);

        JTextArea area = new JTextArea(sw.toString());
        area.setEditable(false);

        JDialog dialog = new JDialog(null, title, block ? ModalityType.APPLICATION_MODAL : ModalityType.DOCUMENT_MODAL);
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(area, new JScrollPane(area));
        dialog.pack();

        dialog.setVisible(true);
    }
    static int calculateChapterId(String mangaId, String volume, double chapter_number, String chapter_title) {
        return new StringBuilder()
                .append(mangaId)
                .append(volume)
                .append(chapter_number)
                .append(chapter_title).hashCode();
    }
    public static InputStream getImageInputStream(String imageName) {
        return ClassLoader.getSystemResourceAsStream("imgs/"+imageName);
    }

    private static final LazyAndWeak<FXMLLoader> fxkeep = new LazyAndWeak<>(FXMLLoader::new);
    public static void fxml(String filename, Object root, Object controller) {
        try {
            FXMLLoader fx = fxkeep.get();
            fx.setLocation(ClassLoader.getSystemResource(filename));
            fx.setController(controller);
            fx.setRoot(root);
            fx.load();
        } catch (IOException e) {
            Utils.showError(null, "fxml failed: "+filename, e, true);
            System.exit(0);
        }
    }
    public static void fxml(Object parentclass, Object root, Object controller) {
        fxml("fxml/"+parentclass.getClass().getSimpleName()+".fxml", root, controller);
    }
    public static void fxml(Object obj) {
        fxml(obj, obj, obj);
    }
    public static void mustNoError(NoError callable) {
        try {
            callable.call();
        } catch (Exception e) {
            showError(null, "error", e, true);
            System.exit(0);
        }
    }
    public static <V> V mustNoError(Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            showError(null, "error", e, true);
            System.exit(0);
        }
        return null;
    }

    @FunctionalInterface
    public static interface NoError {
        public void call() throws Exception;
    }

    public static void showDocument(String url) {
        hostServices.showDocument(url);
    }
    public static int sum(int[] data) {
        int sum = 0;
        for (int i : data) sum += i;
        
        return sum;
    }
    
    public static void stylesheet(Parent node) {
        String name = "stylesheet/"+node.getClass().getSimpleName()+".css"; 
        URL url = ClassLoader.getSystemResource(name);
        if(url == null) {
            LOGGER.severe("stylesheet not found: "+name);
            return;
        }
        node.getStylesheets().add(url.toExternalForm());
    }
    private static EnumMap<State, String> statemap = new EnumMap<>(State.class);
    public static String styleClass(State s) {
        String st = statemap.get(s);
        if(st == null)
            statemap.put(s, st = s.name().toLowerCase());
        
        return st;
    }
    public static State parse(String s) {
        return s == null ? null : State.valueOf(s);
    }
}
