package se.sundsvall.minaarenden;

import se.sundsvall.dept44.ServiceApplication;
import se.sundsvall.dept44.configuration.SecurityConfiguration;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

import static org.springframework.boot.SpringApplication.run;

/**
 * dept44:s SecurityConfiguration släpper igenom alla anrop och ersätts här av API-nyckelskyddet i
 * {@link se.sundsvall.minaarenden.configuration.ApiKeySecurityConfiguration}.
 */
@ServiceApplication(exclude = SecurityConfiguration.class)
@ExcludeFromJacocoGeneratedCoverageReport
public class Application {

	public static void main(final String... args) {
		run(Application.class, args);
	}
}
