package com._4point.aem.watchedfolder.core;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class GitCommit {
	// Build: @git.commit.id.abbrev@ (Build Time: @git.build.time@)
	// log.info("Build Information: Branch '{}', Commit Id '{}'{} ({}), Build Time '{}'.", branch, commitIdAbbrev, dirty ? "*" : "", commitTime, commitBuildTime);
	private final String BRANCH_KEY = "git.branch";
	private final String COMMIT_ABBREV_ID = "git.commit.id.abbrev";
	private final String COMMIT_FULL_ID = "git.commit.id.full";
	private final String DIRTY = "git.dirty";
	private final String COMMIT_TIME = "git.commit.time";
	private final String BUILD_TIME = "git.build.time";
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
}
