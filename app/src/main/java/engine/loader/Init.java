package engine.loader;

import engine.annot.Game;
import engine.core.CoreGame;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Engine loader that discovers and initializes game classes.
 * 
 * Scans the classpath (whether running from IDE or JAR) to find classes annotated with {@link Game}.
 * Validates that the class extends {@link CoreGame} and instantiates it.
 */
public class Init {
    /**
     * Creates a new Init instance.
     * This class is not meant to be instantiated; use {@link #main(String[])} instead.
     */
    public Init() { }

    /**
     * Main entry point for the engine loader.
     * 
     * Scans all classes in the application, finds the first class annotated with {@code @Game}
     * that extends {@link CoreGame}, and starts the game engine.
     * 
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {
        System.out.println("--- Engine Loader Starting ---");

        try {
            // 1. Scan for all classes in the application (Handles both IDE and JAR)
            List<Class<?>> allClasses = getAllClasses();

            boolean gameFound = false;

            for (Class<?> clazz : allClasses) {
                // Requirement 1: Check if class is NOT under engine.*
                if (clazz.getName().startsWith("engine.")) {
                    continue;
                }

                // Requirement 2: Check for @engine.annot.Game annotation
                if (!clazz.isAnnotationPresent(Game.class)) {
                    continue;
                }

                // Requirement 3: Check if it extends/overrides engine.core.CoreGame
                if (!CoreGame.class.isAssignableFrom(clazz)) {
                    System.err.println("Found @Game on " + clazz.getName() + " but it does not extend CoreGame!");
                    continue;
                }

                // Found it!
                System.out.println("Found valid game class: " + clazz.getName());
                Game gameAnnot = clazz.getAnnotation(Game.class);
                System.out.println("Game Name: " + gameAnnot.name());

                gameFound = true;

                // Instantiate and run it
                try {
                    System.out.println("Instantiating...");
                    CoreGame gameInstance = (CoreGame) clazz.getDeclaredConstructor().newInstance();
                    gameInstance.startEngine();
                    // Stop after finding the first game to prevent multiple windows
                    return; 
                } catch (Exception e) {
                    System.err.println("Failed to initialize game: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (!gameFound) {
                System.out.println("No class with @Game annotation found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Scans the classpath to find all loadable classes.
     * Handles both directory (IDE) and JAR (Release) execution modes.
     * 
     * @return a list of all discovered classes
     * @throws Exception if class loading fails
     */
    private static List<Class<?>> getAllClasses() throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        
        // Get the location of THIS class (Init.class)
        URL location = Init.class.getProtectionDomain().getCodeSource().getLocation();
        String path = URLDecoder.decode(location.getFile(), StandardCharsets.UTF_8.name());
        File source = new File(path);

        // Debug info
        System.out.println("Running from: " + source.getAbsolutePath());

        if (source.isDirectory()) {
            // Case 1: Running from IDE (Directories)
            System.out.println("Mode: Directory Scan");
            scanDirectory(source, "", classes);
        } else if (source.getName().endsWith(".jar")) {
            // Case 2: Running from JAR
            System.out.println("Mode: JAR Scan");
            scanJar(source, classes);
        } else {
            System.err.println("Unknown source type: " + source.getName());
        }

        return classes;
    }

    /**
     * Recursively scans a directory for .class files (IDE Mode).
     * 
     * @param directory the directory to scan
     * @param packageName the current package name
     * @param classes the list to accumulate discovered classes
     */
    private static void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Recurse
                String subPackage = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                scanDirectory(file, subPackage, classes);
            } else if (file.getName().endsWith(".class")) {
                try {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    if (className.startsWith(".")) className = className.substring(1);
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    // Ignore invalid classes
                }
            }
        }
    }

    /**
     * Scans all entries inside a JAR file (Release Mode).
     * 
     * @param jarFile the JAR file to scan
     * @param classes the list to accumulate discovered classes
     * @throws Exception if JAR reading fails
     */
    private static void scanJar(File jarFile, List<Class<?>> classes) throws Exception {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // We only care about .class files
                if (!entry.isDirectory() && name.endsWith(".class")) {
                    try {
                        // Convert path "my/project/Game.class" -> "my.project.Game"
                        String className = name.substring(0, name.length() - 6).replace('/', '.');
                        classes.add(Class.forName(className));
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // Sometimes JARs contain metadata classes we can't load, skip them
                    }
                }
            }
        }
    }
}