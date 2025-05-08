package com.jlmorab.ms.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.jlmorab.ms.annotation.IntegrationTest;
import com.jlmorab.ms.config.ContainerBaseTest;
import com.jlmorab.ms.config.TestConfiguration;
import com.jlmorab.ms.config.liquibase.LiquibaseMigration;
import com.jlmorab.ms.config.liquibase.LiquibaseMigrationManager;
import com.jlmorab.ms.utils.LoggerHelper;

@IntegrationTest
@ContextConfiguration(classes = { TestConfiguration.class })
class MigrationIntegrationTest extends ContainerBaseTest {

	static final String PARENT_CHANGELOG_FILE = "db-instance-changelog.xml";
	static final String CHANGELOG_FILE = "db-sales-changelog.xml";
	
	LoggerHelper loggerHelper = LoggerHelper.getInstance();
	
	@Autowired
	DataSource dataSource;
	
	@BeforeEach
	void setUp() {
		loggerHelper.initCapture();
	}//end setUp()
	
	@AfterEach
	void tearDown() {
		loggerHelper.release();
	}//end tearDown()
	
	@Test
	void runDependencyMigration_executeUpdateCorrectly() {
		List<LiquibaseMigration> migrations = List.of( new LiquibaseMigration( 1, PARENT_CHANGELOG_FILE, null ) );
		try {
			assertDoesNotThrow( () -> {
				LiquibaseMigrationManager.resetAndApplyMigration( migrations, dataSource );
			});
			assertThat( loggerHelper.getOutContent() ).contains("Update command completed successfully");
		} finally {
			rollbackMigrations( migrations );
		}//end try
	}//end runDependencyMigration_executeUpdateCorrectly()
	
	@Test
	void runPrincipalMigration_onceTime_executeUpdateCorrectly() {
		List<LiquibaseMigration> migrations = List.of( 
				new LiquibaseMigration( 1, PARENT_CHANGELOG_FILE, null ), 
				new LiquibaseMigration( 2, CHANGELOG_FILE, null ) );
		
		try {
			assertDoesNotThrow( () -> {
				LiquibaseMigrationManager.resetAndApplyMigration( migrations, dataSource );
			});
			
			List<String> changelogs = migrations.stream()
					.map( LiquibaseMigration::getChangeLogFile )
					.toList();
			
			String outContent = loggerHelper.getOutContent();
			
			assertThat( outContent )
				.as("Should contains all changelogs")
				.contains(changelogs);
			assertThat(
				    Arrays.stream( outContent.split( System.lineSeparator() ) )
				          .filter( line -> line.contains("Update command completed successfully") )
				          .count() )
				 .isEqualTo(2);
		} finally {
			rollbackMigrations( migrations );
		}//end try
	}//end runPrincipalMigration_onceTime_executeUpdateCorrectly()
	
	@Test
	void runPrincipalMigration_manyTimes_executeRollbackCorrectly() {
		List<LiquibaseMigration> migrations = List.of( 
				new LiquibaseMigration( 1, PARENT_CHANGELOG_FILE, null ), 
				new LiquibaseMigration( 2, CHANGELOG_FILE, null ),
				new LiquibaseMigration( 3, CHANGELOG_FILE, null ) );
		
		try {
			assertDoesNotThrow( () -> {
				LiquibaseMigrationManager.resetAndApplyMigration( migrations, dataSource );
			});
			
			List<String> changelogs = migrations.stream()
					.map( LiquibaseMigration::getChangeLogFile )
					.toList();
			
			String outContent = loggerHelper.getOutContent();
			
			assertThat( outContent )
				.as("Should contains all changelogs")
				.contains(changelogs);
			assertThat( outContent )
				.contains("Rollback command completed successfully");
			assertThat(
				    Arrays.stream( outContent.split( System.lineSeparator() ) )
				          .filter( line -> line.contains("Update command completed successfully") )
				          .count() )
				 .isEqualTo(3);
		} finally {
			rollbackMigrations( migrations );
		}//end try
	}//end runPrincipalMigration_manyTimes_executeRollbackCorrectly()
	
	
	private void rollbackMigrations( List<LiquibaseMigration> migrations ) {
		migrations.forEach( migration -> {
			try {
				LiquibaseMigrationManager.performRollback( migration, dataSource );
			} catch (Exception e) {
				e.printStackTrace();
			}//end try
		});
	}//end rollbackMigrations()
}
