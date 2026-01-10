package engine.loader;

import engine.annot.Game;
import engine.core.CoreGame;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Init {

    public Init() { }

    public static void main(String[] args) {
        System.out.println("--- Engine Loader Starting ---");

        try {
            // 1. Scan the ENTIRE classpath
            List<Class<?>> allClasses = getAllClasses();

            boolean gameFound = false;

            for (Class<?> clazz : allClasses) {
                // Skip engine internal classes
                if (clazz.getName().startsWith("engine.") || clazz.getName().startsWith("java.")) {
                    continue;
                }

                // Check for Annotation
                if (!clazz.isAnnotationPresent(Game.class)) {
                    continue;
                }

                // Check Inheritance
                if (!CoreGame.class.isAssignableFrom(clazz)) {
                    System.err.println("Found @Game on " + clazz.getName() + " but it does not extend CoreGame!");
                    continue;
                }

                System.out.println("Found valid game class: " + clazz.getName());
                Game gameAnnot = clazz.getAnnotation(Game.class);
                System.out.println("Game Name: " + gameAnnot.name());

                gameFound = true;

                try {
                    System.out.println("Instantiating...");
                    CoreGame gameInstance = (CoreGame) clazz.getDeclaredConstructor().newInstance();
                    gameInstance.startEngine();
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
     * Scans the system classpath string to find all entries.
     */
    private static List<Class<?>> getAllClasses() {
        List<Class<?>> classes = new ArrayList<>();
        
        // This property contains all JARs and folders loaded by the application
        String classPath = System.getProperty("java.class.path");
        String[] pathEntries = classPath.split(File.pathSeparator);

        for (String entry : pathEntries) {
            File file = new File(entry);
            try {
                if (file.isDirectory()) {
                    scanDirectory(file, "", classes);
                } else if (file.getName().endsWith(".jar")) {
                    scanJar(file, classes);
                }
            } catch (Exception e) {
                // If one classpath entry fails (e.g. corrupted jar), just skip it
                System.err.println("Failed to scan classpath entry: " + entry);
            }
        }

        return classes;
    }

    private static void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                scanDirectory(file, subPackage, classes);
            } else if (file.getName().endsWith(".class")) {
                try {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    if (className.startsWith(".")) className = className.substring(1);
                    // Optimization: Don't load module-info or obvious system classes
                    if (!className.equals("module-info")) {
                        classes.add(Class.forName(className, false, Init.class.getClassLoader()));
                    }
                } catch (Throwable e) {
                    // Ignore classes that cannot be loaded (missing dependencies etc)
                }
            }
        }
    }

    private static void scanJar(File jarFile, List<Class<?>> classes) throws Exception {
        // Optimization: Skip scanning the standard Java runtime JARs (rt.jar, etc) to speed up boot
        // You can add logic here to check if jarFile.getName() contains "jdk" or "jre"
        
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!entry.isDirectory() && name.endsWith(".class")) {
                    try {
                        String className = name.substring(0, name.length() - 6).replace('/', '.');
                        // Optimization: Skip module-info and engine classes immediately
                        if (!className.equals("module-info") && !className.startsWith("engine.")) {
                             // false flag = don't initialize static blocks yet, safer for scanning
                            classes.add(Class.forName(className, false, Init.class.getClassLoader()));
                        }
                    } catch (Throwable e) {
                        // Ignore
                    }
                }
            }
        }
    }
}