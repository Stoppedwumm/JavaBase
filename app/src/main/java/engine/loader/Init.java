package engine.loader;

import engine.annot.Game;
import engine.core.CoreGame;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Init {

    public static void main(String[] args) {
        System.out.println("--- Engine Loader Starting ---");
        try {
            List<Class<?>> allClasses = getClasses("");
            
            for (Class<?> clazz : allClasses) {
                // Check exclusion and annotation
                if (clazz.getName().startsWith("engine.") || !clazz.isAnnotationPresent(Game.class)) {
                    continue;
                }

                if (!CoreGame.class.isAssignableFrom(clazz)) {
                    System.err.println("Class " + clazz.getName() + " has @Game but doesn't extend CoreGame");
                    continue;
                }

                System.out.println("Launching: " + clazz.getAnnotation(Game.class).name());
                
                // Instantiate and Start
                CoreGame game = (CoreGame) clazz.getDeclaredConstructor().newInstance();
                
                // This method sets up the JFrame and starts the Loop thread
                // The main method ends here, but the Swing Thread keeps JVM alive.
                game.startEngine(); 
                return; // Stop after finding the first game
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Same Scanning Logic as previous response ---
    private static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) dirs.add(new File(resources.nextElement().getFile()));
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) classes.addAll(findClasses(directory, packageName));
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) return classes;
        File[] files = directory.listFiles();
        if (files == null) return classes;
        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                classes.addAll(findClasses(file, subPackage));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                if (className.startsWith(".")) className = className.substring(1);
                classes.add(Class.forName(className));
            }
        }
        return classes;
    }
}