package com._4point.aem.watchedfolder.core;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that works in tandem with the Git Commit Id plugin (https://github.com/git-commit-id/git-commit-id-maven-plugin)
 * 
 * The class is used to retrieve and log information from a properties file that was generated from the Git Commit Id plugin.
 *
 */
public class GitCommit {
	private static final Logger log = LoggerFactory.getLogger(GitCommit.class);

	private static final String DEFAULT_GIT_PROPERTIES_FILENAME = "/git.properties";

	private static final String BRANCH_KEY = "git.branch";
	private static final String COMMIT_ABBREV_ID = "git.commit.id.abbrev";
	private static final String COMMIT_FULL_ID = "git.commit.id.full";
	private static final String DIRTY = "git.dirty";
	private static final String COMMIT_TIME = "git.commit.time";
	private static final String BUILD_TIME = "git.build.time";
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
	
	private final Properties gitProps;

	public GitCommit(Properties gitProps) {
		this.gitProps = gitProps;
	}
	
	public String branch() {
		return gitProps.getProperty(BRANCH_KEY);
	}
	
	public String commitIdAbbrev() {
		return gitProps.getProperty(COMMIT_ABBREV_ID);
	}

	public String commitIdFull() {
		return gitProps.getProperty(COMMIT_FULL_ID);
	}

	public boolean dirty() {
		return Boolean.valueOf(gitProps.getProperty(DIRTY));
	}

	public ZonedDateTime commitTime() {
		return convertToLocalTimeZone(gitProps.getProperty(COMMIT_TIME));
	}

	public ZonedDateTime buildTime() {
		return convertToLocalTimeZone(gitProps.getProperty(BUILD_TIME));
	}
	
	public String summary() {
		return String.format("Build Information: Branch '%s', Commit Id '%s'%s (%s), Build Time '%s'.", 
								branch(), 
								commitIdAbbrev(), 
								dirty() ? "*" : "", 
								DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(commitTime()), 
								DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(buildTime()));
	}

	private ZonedDateTime convertToLocalTimeZone(String property) {
		return java.time.ZonedDateTime.parse(property, DATETIME_FORMATTER).withZoneSameInstant(ZoneId.systemDefault());
	}
	
	/**
	 * Creates a GitCommit object from a properties file that is retrieved using a class's getResourceAsStream method.  
	 * 
	 * @param propertiesResourceName	name of the properties resource (starting with / for absolute parh otherwise relative to clazz
	 * @param clazz						class to be used to load the resource
	 * @return							created GitCommit object
	 * @throws IOException
	 */
	public static GitCommit from(String propertiesResourceName, Class<?> clazz) throws IOException {
		Properties gitProperties = new Properties();
		gitProperties.load(clazz.getResourceAsStream(propertiesResourceName));
		return new GitCommit(gitProperties);
	}
	
	/**
	 * Creates a GitCommit object from a properties file that is retrieved the GitCommit classes getResourceAsStream method.  
	 * 
	 * @param propertiesResourceName	name of the properties resource (starting with / for absolute parh otherwise relative to is class
	 * @return							created GitCommit object
	 * @throws IOException
	 */
	public static GitCommit from(String propertiesResourceName) throws IOException {
		return from(propertiesResourceName, GitCommit.class);
	}
	
	/**
	 * Creates a GitCommit object from a properties file that is retrieved the GitCommit classes getResourceAsStream method
	 * using the GitCommit plugin's default name ("/git.properties").   
	 * 
	 * @return							created GitCommit object
	 * @throws IOException
	 */
	public static GitCommit getDefault() throws IOException {
		return GitCommit.from(DEFAULT_GIT_PROPERTIES_FILENAME);
	}
	
	/**
	 * Logs a summary of the build information to the log using a specified properties resource relative to a given class.
	 * 
	 * @param propertiesResourceName	Name of the properties file resrouce
	 * @param clazz						Class that this is relative to.
	 */
	public static void logSummary(String propertiesResourceName, Class<?> clazz) {
		try {
			log.info(GitCommit.from(propertiesResourceName, clazz).summary());
		} catch (IOException e) {
			log.warn("Unable to load for properties from '{}'.", propertiesResourceName);
		}
	}
	
	/**
	 * Logs a summary of the build information to the log using a specified properties resource relative this class.
	 * 
	 * @param propertiesResourceName
	 */
	public static void logSummary(String propertiesResourceName) {
		logSummary(propertiesResourceName, GitCommit.class);
	}
	
	/**
	 * Logs a summary of the build information to the log using the default properties location.
	 */
	public static void logSummary() {
		logSummary(DEFAULT_GIT_PROPERTIES_FILENAME);
	}
}
