///usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS mavencentral,github=https://maven.pkg.github.com/rmcdouga/*
//DEPS info.picocli:picocli:4.6.3
//DEPS com.github.rmcdouga:github-package-repo:0.0.1-SNAPSHOT
//DEPS org.slf4j:slf4j-simple:1.7.36
//JAVA 17+

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

import com.github.rmcdouga.ghrepo.GithubPackages;
import com.github.rmcdouga.ghrepo.GithubPackages.Repo.Group.Artifact.Version;

@Command(name = "GrabJars", mixinStandardHelpOptions = true, version = "GrabJars 0.1",
        description = "GrabJars will grab .jar files from the Watched Folder Utils repositories.")
class GrabJars implements Callable<Integer> {

	@Parameters(index = "0", description = "Destination directory where .jars will be written.", defaultValue = ".")
    private String desintationDir;

    public static void main(String... args) {
        int exitCode = new CommandLine(new GrabJars()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
    	final Path dest = Path.of(desintationDir);
    	GithubPackages packages = GithubPackages.create().verboseMode(true);
		packages.repo("4PointSolutions", "WatchedFolderUtils")
				.group("com.4point.aem.watchedfolder")
				.artifact("watched-folder-poster")
				.version("0.0.1-SNAPSHOT")
				.copyTo(dest, StandardCopyOption.REPLACE_EXISTING);

//		packages.repo("4PointSolutions", "WatchedFolderUtils")
//				.group("com.4point.aem.watchedfolder")
//				.artifact("watched-no-op")
//				.version("0.0.1-SNAPSHOT")
//				.copyTo(dest, StandardCopyOption.REPLACE_EXISTING);

//		packages.repo("4PointSolutions", "WatchedFolderUtils")
//				.group("com.4point.aem.watchedfolder")
//				.artifact("mock-client")
//				.version("0.0.1-SNAPSHOT")
//				.copyTo(dest, StandardCopyOption.REPLACE_EXISTING);

        return 0;
    }
}
